/*
 * This file is part of JuniperBotJ.
 *
 * JuniperBotJ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * JuniperBotJ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with JuniperBotJ. If not, see <http://www.gnu.org/licenses/>.
 */
package ru.juniperbot.worker.common.moderation.service;

import lombok.NonNull;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.collections4.CollectionUtils;
import org.joda.time.DateTime;
import org.joda.time.Minutes;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.juniperbot.worker.common.audit.model.AuditActionBuilder;
import ru.juniperbot.common.model.AuditActionType;
import ru.juniperbot.worker.common.audit.service.AuditService;
import ru.juniperbot.worker.common.event.service.ContextService;
import ru.juniperbot.worker.common.moderation.model.ModerationActionRequest;
import ru.juniperbot.common.model.ModerationActionType;
import ru.juniperbot.common.persistence.entity.ModerationConfig;
import ru.juniperbot.common.persistence.entity.MuteState;
import ru.juniperbot.common.persistence.repository.MuteStateRepository;
import ru.juniperbot.common.service.ModerationConfigService;
import ru.juniperbot.worker.common.jobs.UnMuteJob;
import ru.juniperbot.worker.common.shared.service.DiscordEntityAccessor;

import java.awt.*;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import static ru.juniperbot.worker.common.audit.provider.ModerationAuditForwardProvider.*;

@Service
public class MuteServiceImpl implements MuteService {

    private final static String MUTED_ROLE_NAME = "JB-MUTED";

    private enum PermissionMode {
        DENY, ALLOW, UNCHECKED
    }

    @Autowired
    private ContextService contextService;

    @Autowired
    private ModerationConfigService configService;

    @Autowired
    private MuteStateRepository muteStateRepository;

    @Autowired
    private SchedulerFactoryBean schedulerFactoryBean;

    @Autowired
    private AuditService auditService;

    @Override
    @Transactional
    public Role getMutedRole(Guild guild) {
        return getMutedRole(guild, true);
    }

    private Role getMutedRole(Guild guild, boolean create) {
        ModerationConfig moderationConfig = create
                ? configService.getOrCreate(guild.getIdLong())
                : configService.getByGuildId(guild.getIdLong());

        Role role = null;
        if (moderationConfig != null && moderationConfig.getMutedRoleId() != null) {
            role = guild.getRoleById(moderationConfig.getMutedRoleId());
        }

        if (role == null) {
            List<Role> mutedRoles = guild.getRolesByName(MUTED_ROLE_NAME, true);
            role = CollectionUtils.isNotEmpty(mutedRoles) ? mutedRoles.get(0) : null;
        }

        if (create && (role == null || !guild.getSelfMember().canInteract(role))) {
            role = guild.createRole()
                    .setColor(Color.GRAY)
                    .setMentionable(false)
                    .setName(MUTED_ROLE_NAME)
                    .complete();
        }

        if (role != null) {
            if (moderationConfig != null && !Objects.equals(moderationConfig.getMutedRoleId(), role.getIdLong())) {
                moderationConfig.setMutedRoleId(role.getIdLong());
                configService.save(moderationConfig);
            }

            for (TextChannel channel : guild.getTextChannels()) {
                checkPermission(channel, role, PermissionMode.DENY, Permission.MESSAGE_WRITE);
            }
            for (VoiceChannel channel : guild.getVoiceChannels()) {
                checkPermission(channel, role, PermissionMode.DENY, Permission.VOICE_SPEAK);
            }
        }
        return role;
    }

    @Override
    @Transactional
    public boolean mute(ModerationActionRequest request) {

        AuditActionBuilder actionBuilder = request.isAuditLogging() ? auditService
                .log(request.getGuild(), AuditActionType.MEMBER_MUTE)
                .withUser(request.getModerator())
                .withTargetUser(request.getViolator())
                .withChannel(request.isGlobal() ? null : request.getChannel())
                .withAttribute(REASON_ATTR, request.getReason())
                .withAttribute(DURATION_ATTR, request.getDuration())
                .withAttribute(GLOBAL_ATTR, request.isGlobal()) : null;

        Consumer<Object> schedule = g -> {
            contextService.inTransaction(() -> {
                if (!request.isStateless()) {
                    if (request.getDuration() != null) {
                        scheduleUnMute(request);
                    }
                    storeState(request);
                }
                if (actionBuilder != null) {
                    actionBuilder.save();
                }
            });
        };

        if (request.isGlobal()) {
            Guild guild = request.getGuild();
            Role mutedRole = getMutedRole(guild);
            if (!request.getViolator().getRoles().contains(mutedRole)) {
                guild.addRoleToMember(request.getViolator(), mutedRole)
                        .queue(e -> schedule.accept(null));
                return true;
            }
        } else {
            PermissionOverride override = request.getChannel().getPermissionOverride(request.getViolator());
            if (override != null && override.getDenied().contains(Permission.MESSAGE_WRITE)) {
                return false;
            }
            if (override == null) {
                request.getChannel().createPermissionOverride(request.getViolator())
                        .setDeny(Permission.MESSAGE_WRITE)
                        .queue(e -> schedule.accept(null));
            } else {
                override.getManager().deny(Permission.MESSAGE_WRITE).queue(e -> schedule.accept(false));
            }
            return true;
        }
        return false;
    }

    @Override
    @Transactional
    public boolean unmute(Member author, TextChannel channel, Member member) {
        Guild guild = member.getGuild();
        boolean result = false;
        Role mutedRole = getMutedRole(guild);
        if (member.getRoles().contains(mutedRole)) {
            guild.removeRoleFromMember(member, mutedRole).queue();
            result = true;
        }
        if (channel != null) {
            PermissionOverride override = channel.getPermissionOverride(member);
            if (override != null) {
                override.delete().queue();
                result = true;
            }
        }
        removeUnMuteSchedule(member, channel);
        if (result) {
            auditService
                    .log(guild, AuditActionType.MEMBER_UNMUTE)
                    .withUser(author)
                    .withTargetUser(member)
                    .withChannel(channel)
                    .save();
        }
        return result;
    }

    @Override
    @Transactional
    @Async
    public void refreshMute(Member member) {
        Scheduler scheduler = schedulerFactoryBean.getScheduler();
        try {
            List<MuteState> muteStates = muteStateRepository.findAllByGuildIdAndUserId(member.getGuild().getIdLong(), member.getUser().getId());
            if (CollectionUtils.isNotEmpty(muteStates)) {
                muteStates.stream().filter(e -> !processState(member, e)).forEach(muteStateRepository::delete);
                return;
            }

            JobKey key = UnMuteJob.getKey(member);
            if (!scheduler.checkExists(key)) {
                return;
            }
            JobDetail detail = scheduler.getJobDetail(key);
            if (detail == null) {
                return;
            }
            JobDataMap data = detail.getJobDataMap();
            boolean global = data.getBoolean(UnMuteJob.ATTR_GLOBAL_ID);
            String channelId = data.getString(UnMuteJob.ATTR_CHANNEL_ID);
            TextChannel textChannel = channelId != null ? member.getGuild().getTextChannelById(channelId) : null;
            if (global || textChannel != null) {
                ModerationActionRequest request = ModerationActionRequest.builder()
                        .type(ModerationActionType.MUTE)
                        .channel(textChannel)
                        .violator(member)
                        .global(global)
                        .auditLogging(false)
                        .build();
                mute(request);
            }
        } catch (SchedulerException e) {
            // fall down, we don't care
        }
    }

    @Override
    @Transactional
    public boolean isMuted(@NonNull Member member, @NonNull TextChannel channel) {
        Role mutedRole = getMutedRole(member.getGuild(), false);
        if (mutedRole != null && member.getRoles().contains(mutedRole)) {
            return true;
        }

        DateTime now = DateTime.now();
        for (MuteState state : muteStateRepository.findAllByGuildIdAndUserId(member.getGuild().getIdLong(), member.getUser().getId())) {
            DateTime expire = state.getExpire() != null ? new DateTime(state.getExpire()) : null;
            if (expire != null && now.isAfter(expire)) {
                continue;
            }
            if (!state.isGlobal() && Objects.equals(state.getChannelId(), channel.getId())) {
                return true;
            }
        }
        return false;
    }

    @Override
    @Transactional
    public void clearState(long guildId, String userId, String channelId) {
        muteStateRepository.deleteByGuildIdAndUserIdAndChannelId(guildId, userId, channelId);
    }

    private static void checkPermission(GuildChannel channel, Role role, PermissionMode mode, Permission permission) {
        PermissionOverride override = channel.getPermissionOverride(role);
        if (!channel.getGuild().getSelfMember().hasPermission(channel, Permission.MANAGE_PERMISSIONS)) {
            return;
        }
        if (override == null) {
            switch (mode) {
                case DENY:
                    channel.createPermissionOverride(role).setDeny(permission).queue();
                    break;
                case ALLOW:
                    channel.createPermissionOverride(role).setAllow(permission).queue();
                    break;
                case UNCHECKED:
                    // do nothing
                    break;
            }
        } else {
            switch (mode) {
                case DENY:
                    if (!override.getDenied().contains(permission)) {
                        override.getManager().deny(permission).queue();
                    }
                    break;
                case ALLOW:
                    if (!override.getAllowed().contains(permission)) {
                        override.getManager().grant(permission).queue();
                    }
                    break;
                case UNCHECKED:
                    // do nothing
                    break;
            }
        }
    }

    private void scheduleUnMute(ModerationActionRequest request) {
        try {
            removeUnMuteSchedule(request.getViolator(), request.getChannel());
            JobDetail job = UnMuteJob.createDetails(request.isGlobal(), request.getChannel(), request.getViolator());
            Trigger trigger = TriggerBuilder
                    .newTrigger()
                    .startAt(DateTime.now().plusMinutes(request.getDuration()).toDate())
                    .withSchedule(SimpleScheduleBuilder.simpleSchedule())
                    .build();
            schedulerFactoryBean.getScheduler().scheduleJob(job, trigger);
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }
    }

    private void removeUnMuteSchedule(Member member, TextChannel channel) {
        Scheduler scheduler = schedulerFactoryBean.getScheduler();
        try {
            JobKey key = UnMuteJob.getKey(member);
            if (scheduler.checkExists(key)) {
                scheduler.deleteJob(key);
                muteStateRepository.deleteByGuildIdAndUserId(member.getGuild().getIdLong(), member.getUser().getId());
            }
            if (channel != null) {
                key = UnMuteJob.getKey(member, channel);
                if (scheduler.checkExists(key)) {
                    scheduler.deleteJob(key);
                }
                muteStateRepository.deleteByGuildIdAndUserIdAndChannelId(member.getGuild().getIdLong(),
                        member.getUser().getId(), channel.getId()); // remove it even if job non exists
            }
        } catch (SchedulerException e) {
            // fall down, we don't care
        }
    }

    private void storeState(ModerationActionRequest request) {
        MuteState state = new MuteState();
        state.setGlobal(request.isGlobal());
        state.setUserId(request.getViolator().getUser().getId());
        state.setGuildId(request.getViolator().getGuild().getIdLong());
        DateTime dateTime = DateTime.now();
        if (request.getDuration() != null) {
            dateTime = dateTime.plusMinutes(request.getDuration());
        } else {
            dateTime = dateTime.plusYears(100);
        }
        state.setExpire(dateTime.toDate());
        state.setReason(request.getReason());
        if (request.getChannel() != null) {
            state.setChannelId(request.getChannel().getId());
        }
        muteStateRepository.save(state);
    }

    private boolean processState(Member member, MuteState muteState) {
        DateTime now = DateTime.now();
        DateTime expire = muteState.getExpire() != null ? new DateTime(muteState.getExpire()) : null;
        if (expire != null && now.isAfter(expire)) {
            return false;
        }

        TextChannel textChannel = muteState.getChannelId() != null ? member.getGuild().getTextChannelById(muteState.getChannelId()) : null;
        if (!muteState.isGlobal() && textChannel == null) {
            return false;
        }

        Integer duration = expire != null ? Minutes.minutesBetween(expire, now).getMinutes() : null;
        ModerationActionRequest request = ModerationActionRequest.builder()
                .type(ModerationActionType.MUTE)
                .channel(textChannel)
                .violator(member)
                .global(muteState.isGlobal())
                .duration(duration)
                .reason(muteState.getReason())
                .stateless(true)
                .auditLogging(false)
                .build();
        mute(request);
        return true;
    }
}
