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
package ru.caramel.juniperbot.module.moderation.service;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.managers.GuildController;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.Minutes;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.caramel.juniperbot.core.persistence.entity.LocalMember;
import ru.caramel.juniperbot.core.service.*;
import ru.caramel.juniperbot.core.service.impl.AbstractDomainServiceImpl;
import ru.caramel.juniperbot.core.support.RequestScopedCacheManager;
import ru.caramel.juniperbot.core.utils.CommonUtils;
import ru.caramel.juniperbot.module.moderation.jobs.UnMuteJob;
import ru.caramel.juniperbot.module.moderation.model.SlowMode;
import ru.caramel.juniperbot.module.moderation.persistence.entity.MemberWarning;
import ru.caramel.juniperbot.module.moderation.persistence.entity.ModerationConfig;
import ru.caramel.juniperbot.module.moderation.persistence.entity.MuteState;
import ru.caramel.juniperbot.module.moderation.persistence.repository.MemberWarningRepository;
import ru.caramel.juniperbot.module.moderation.persistence.repository.ModerationConfigRepository;
import ru.caramel.juniperbot.module.moderation.persistence.repository.MuteStateRepository;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
public class ModerationServiceImpl
        extends AbstractDomainServiceImpl<ModerationConfig, ModerationConfigRepository>
        implements ModerationService {

    private final static String MUTED_ROLE_NAME = "JB-MUTED";

    private final static String COLOR_ROLE_NAME = "JB-CLR-";

    @Autowired
    private MemberWarningRepository warningRepository;

    @Autowired
    private MuteStateRepository muteStateRepository;

    @Autowired
    private MemberService memberService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private ContextService contextService;

    @Autowired
    private SchedulerFactoryBean schedulerFactoryBean;

    private Map<Long, SlowMode> slowModeMap = new ConcurrentHashMap<>();

    public ModerationServiceImpl(@Autowired ModerationConfigRepository repository) {
        super(repository, true);
    }

    @Override
    protected ModerationConfig createNew(long guildId) {
        return new ModerationConfig(guildId);
    }

    @Override
    @Cacheable(value = "ModerationServiceImpl.isModerator", cacheManager = RequestScopedCacheManager.NAME)
    public boolean isModerator(Member member) {
        if (member == null) {
            return false;
        }
        if (member.hasPermission(Permission.ADMINISTRATOR) || member.isOwner()) {
            return true;
        }
        ModerationConfig config = get(member.getGuild());
        return config != null && CollectionUtils.isNotEmpty(config.getRoles())
                && member.getRoles().stream().anyMatch(e -> config.getRoles().contains(e.getIdLong()));
    }

    @Override
    public boolean isPublicColor(long guildId) {
        ModerationConfig config = getByGuildId(guildId);
        return config != null && config.isPublicColors();
    }

    @Override
    public boolean setColor(Member member, String color) {
        Role role = null;
        Guild guild = member.getGuild();
        Member self = guild.getSelfMember();

        GuildController controller = member.getGuild().getController();

        if (StringUtils.isNotEmpty(color)) {
            String roleName = COLOR_ROLE_NAME + color;
            List<Role> roles = member.getGuild().getRolesByName(roleName, false);
            role = roles.stream().filter(self::canInteract).findFirst().orElse(null);
            if (role == null) {
                role = controller
                        .createRole()
                        .setColor(CommonUtils.hex2Rgb(color))
                        .setMentionable(false)
                        .setName(roleName)
                        .complete();

                Role highestRole = CommonUtils.getHighestRole(self);
                if (highestRole != null) {
                    controller.modifyRolePositions()
                            .selectPosition(role)
                            .moveUp(highestRole.getPosition() - role.getPosition() - 1)
                            .complete();
                }
            }

            if (!self.canInteract(role)) {
                return false;
            }
        }

        if (role == null || !member.getRoles().contains(role)) {
            List<Role> roleList = member.getRoles().stream()
                    .filter(e -> e.getName().startsWith(COLOR_ROLE_NAME))
                    .filter(self::canInteract)
                    .collect(Collectors.toList());
            if (role != null) {
                if (CollectionUtils.isEmpty(roleList)) {
                    controller.addRolesToMember(member, role).complete();
                } else {
                    controller.modifyMemberRoles(member, Collections.singleton(role), roleList).complete();
                }
            } else {
                controller.removeRolesFromMember(member, roleList).complete();
            }
        }
        // remove unused color roles
        Set<Role> userRoles = new LinkedHashSet<>();
        if (role != null) {
            userRoles.add(role);
        }
        guild.getMembers().forEach(m -> userRoles.addAll(m.getRoles()));
        guild.getRoles().stream()
                .filter(e -> e.getName().startsWith(COLOR_ROLE_NAME) && !userRoles.contains(e) && self.canInteract(e))
                .forEach(e -> e.delete().queue());
        return true;
    }

    @Override
    public Role getMutedRole(Guild guild) {
        List<Role> mutedRoles = guild.getRolesByName(MUTED_ROLE_NAME, true);
        Role role = CollectionUtils.isNotEmpty(mutedRoles) ? mutedRoles.get(0) : null;
        if (role == null || !guild.getSelfMember().canInteract(role)) {
            role = guild.getController()
                    .createRole()
                    .setColor(Color.GRAY)
                    .setMentionable(false)
                    .setName(MUTED_ROLE_NAME)
                    .complete();
        }
        for (TextChannel channel : guild.getTextChannels()) {
            checkPermission(channel, role, PermissionMode.DENY, Permission.MESSAGE_WRITE);
        }
        for (VoiceChannel channel : guild.getVoiceChannels()) {
            // remove muted state for existing roles
            checkPermission(channel, role, PermissionMode.UNCHECKED, Permission.VOICE_SPEAK);
        }
        return role;
    }

    private enum PermissionMode {
        DENY, ALLOW, UNCHECKED
    }

    private static void checkPermission(Channel channel, Role role, PermissionMode mode, Permission permission) {
        PermissionOverride override = channel.getPermissionOverride(role);
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
                case UNCHECKED:
                case ALLOW:
                    if (!override.getAllowed().contains(permission)) {
                        override.getManager().clear(permission).queue();
                    }
                    break;
            }
        }
    }

    @Override
    public boolean mute(TextChannel channel, Member member, boolean global, Integer duration, String reason) {
        return mute(channel, member, global, duration, reason, false);
    }

    private boolean mute(TextChannel channel, Member member, boolean global, Integer duration, String reason, boolean stateless) {
        // TODO reason will be implemented in audit
        if (global) {
            Role mutedRole = getMutedRole(member.getGuild());
            if (!member.getRoles().contains(mutedRole)) {
                member.getGuild()
                        .getController()
                        .addRolesToMember(member, mutedRole)
                        .queue();
                member.getGuild().getController().setMute(member, true).queue(e -> {
                    if (!stateless) {
                        if (duration != null) {
                            scheduleUnMute(true, channel, member, duration);
                        }
                        storeState(true, channel, member, duration, reason);
                    }

                });
                return true;
            }
        } else {
            PermissionOverride override = channel.getPermissionOverride(member);
            if (override != null && !override.getDenied().contains(Permission.MESSAGE_WRITE)) {
                override.getManager().deny(Permission.MESSAGE_WRITE).queue();
                return true;
            }
            if (override == null) {
                channel.createPermissionOverride(member)
                        .setDeny(Permission.MESSAGE_WRITE)
                        .queue(e -> {
                            if (!stateless) {
                                if (duration != null) {
                                    scheduleUnMute(false, channel, member, duration);
                                }
                                storeState(false, channel, member, duration, reason);
                            }

                        });
                return true;
            }
        }
        return false;
    }

    @Override
    @Transactional
    public boolean unmute(TextChannel channel, Member member) {
        boolean result = false;
        Role mutedRole = getMutedRole(member.getGuild());
        if (member.getRoles().contains(mutedRole)) {
            member.getGuild()
                    .getController()
                    .removeRolesFromMember(member, mutedRole)
                    .queue();
            member.getGuild().getController().setMute(member, false).queue();
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
        return result;
    }

    @Override
    @Transactional
    @Async
    public void refreshMute(Member member) {
        Scheduler scheduler = schedulerFactoryBean.getScheduler();
        try {
            List<MuteState> muteStates = muteStateRepository.findAllByMember(member);
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
                mute(textChannel, member, global, null, null);
            }
        } catch (SchedulerException e) {
            // fall down, we don't care
        }
    }

    private void scheduleUnMute(boolean global, TextChannel channel, Member member, int duration) {
        try {
            removeUnMuteSchedule(member, channel);
            JobDetail job = UnMuteJob.createDetails(global, channel, member);
            Trigger trigger = TriggerBuilder
                    .newTrigger()
                    .startAt(DateTime.now().plusMinutes(duration).toDate())
                    .withSchedule(SimpleScheduleBuilder.simpleSchedule())
                    .build();
            schedulerFactoryBean.getScheduler().scheduleJob(job, trigger);
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }
    }

    private void storeState(boolean global, TextChannel channel, Member member, Integer duration, String reason) {
        MuteState state = new MuteState();
        state.setGlobal(global);
        state.setUserId(member.getUser().getId());
        state.setGuildId(member.getGuild().getIdLong());
        DateTime dateTime = DateTime.now();
        if (duration != null) {
            dateTime = dateTime.plusMinutes(duration);
        } else {
            dateTime = dateTime.plusYears(100);
        }
        state.setExpire(dateTime.toDate());
        state.setReason(reason);
        if (channel != null) {
            state.setChannelId(channel.getId());
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
        mute(textChannel, member, muteState.isGlobal(), duration, muteState.getReason(), true);
        return true;
    }

    private void removeUnMuteSchedule(Member member, TextChannel channel) {
        Scheduler scheduler = schedulerFactoryBean.getScheduler();
        try {
            JobKey key = UnMuteJob.getKey(member);
            if (scheduler.checkExists(key)) {
                scheduler.deleteJob(key);
                muteStateRepository.deleteByMember(member);
            }
            if (channel != null) {
                key = UnMuteJob.getKey(member, channel);
                if (scheduler.checkExists(key)) {
                    scheduler.deleteJob(key);
                }
                muteStateRepository.deleteByMember(member, channel.getId()); // remove it even if job non exists
            }
        } catch (SchedulerException e) {
            // fall down, we don't care
        }
    }

    @Override
    public void slowMode(TextChannel channel, int interval) {
        SlowMode slowMode = slowModeMap.computeIfAbsent(channel.getIdLong(), e -> {
            SlowMode result = new SlowMode();
            result.setChannelId(e);
            return result;
        });
        slowMode.setInterval(interval);
    }

    @Override
    public boolean isRestricted(TextChannel channel, Member member) {
        if (member == null || member.getUser().isBot() || isModerator(member)) {
            return false;
        }
        SlowMode slowMode = slowModeMap.get(channel.getIdLong());
        return slowMode != null && slowMode.tick(member.getUser().getId());
    }

    @Override
    public boolean slowOff(TextChannel channel) {
        return slowModeMap.remove(channel.getIdLong()) != null;
    }

    @Override
    public boolean kick(Member author, Member member) {
        return kick(author, member, null);
    }

    @Override
    public boolean kick(Member author, Member member, final String reason) {
        Member self = member.getGuild().getSelfMember();
        if (self.hasPermission(Permission.KICK_MEMBERS) && self.canInteract(member)) {
            String reasonAuthor = StringUtils.isNotEmpty(reason)
                    ? String.format("%s: %s", author.getEffectiveName(), reason)
                    : author.getEffectiveName();
            notifyUserAction(e -> {
                member.getGuild().getController().kick(member, reasonAuthor).queue();
            }, member, "discord.command.mod.action.message.kick", reason);
            return true;
        }
        return false;
    }

    @Override
    public boolean ban(Member author, Member member) {
        return ban(author, member, null);
    }

    @Override
    public boolean ban(Member author, Member member, String reason) {
        return ban(author, member, 0, reason);
    }

    @Override
    public boolean ban(Member author, Member member, int delDays, final String reason) {
        Member self = member.getGuild().getSelfMember();
        if (self.hasPermission(Permission.BAN_MEMBERS) && self.canInteract(member)) {
            String reasonAuthor = StringUtils.isNotEmpty(reason)
                    ? String.format("%s: %s", author.getEffectiveName(), reason)
                    : author.getEffectiveName();
            notifyUserAction(e -> {
                member.getGuild().getController().ban(member, delDays, reasonAuthor).queue();
            }, member, "discord.command.mod.action.message.ban", reason);
            return true;
        }
        return false;
    }

    @Override
    @Transactional
    public List<MemberWarning> getWarnings(Member member) {
        LocalMember localMember = memberService.getOrCreate(member);
        return warningRepository.findActiveByViolator(member.getGuild().getIdLong(), localMember);
    }

    @Override
    public boolean warn(Member author, Member member) {
        return warn(author, member, null);
    }

    @Override
    @Transactional
    public long warnCount(Member member) {
        LocalMember memberLocal = memberService.getOrCreate(member);
        return warningRepository.countActiveByViolator(member.getGuild().getIdLong(), memberLocal);
    }

    @Override
    @Transactional
    public boolean warn(Member author, Member member, String reason) {
        long guildId = member.getGuild().getIdLong();
        ModerationConfig moderationConfig = getOrCreate(member.getGuild());
        LocalMember authorLocal = memberService.getOrCreate(author);
        LocalMember memberLocal = memberService.getOrCreate(member);

        long count = warningRepository.countActiveByViolator(guildId, memberLocal);
        boolean exceed = count >= moderationConfig.getMaxWarnings() - 1;
        MemberWarning warning = new MemberWarning(guildId, authorLocal, memberLocal, reason);
        if (exceed) {
            reason = messageService.getMessage("discord.command.mod.warn.exceeded", count);
            boolean success = true;
            switch (moderationConfig.getWarnExceedAction()) {
                case BAN:
                    success = ban(author, member, reason);
                    break;
                case KICK:
                    success = kick(author, member, reason);
                    break;
                case MUTE:
                    mute(null, member, true, moderationConfig.getMuteCount(), reason);
                    break;
            }
            if (success) {
                warningRepository.flushWarnings(guildId, memberLocal);
                warning.setActive(false);
            }
        } else {
            notifyUserAction(e -> {}, member, "discord.command.mod.action.message.warn", reason, count + 1,
                    moderationConfig.getMaxWarnings());
        }
        warningRepository.save(warning);
        return exceed;
    }

    @Override
    @Transactional
    public void removeWarn(MemberWarning warning) {
        Objects.requireNonNull(warning, "No warning specified to remove");
        warning.setActive(false);
        warningRepository.save(warning);
    }

    @Override
    @Transactional
    public void clearState(long guildId, String userId, String channelId) {
        muteStateRepository.deleteByGuildIdAndUserIdAndChannelId(guildId, userId, channelId);
    }

    private void notifyUserAction(Consumer<Void> consumer, Member member, String code, String reason, Object... objects) {
        if (StringUtils.isEmpty(reason)) {
            code += ".noReason";
        }
        if (member.getUser().isBot()) {
            return; // do not notify bots
        }
        String finalCode = code;
        try {
            member.getUser().openPrivateChannel().queue(e -> {
                contextService.withContext(member.getGuild(), () -> {
                    Object[] args = new Object[] { member.getGuild().getName() };
                    if (ArrayUtils.isNotEmpty(objects)) {
                        args = ArrayUtils.addAll(args, objects);
                    }
                    if (StringUtils.isNotEmpty(reason)) {
                        args = ArrayUtils.add(args, reason);
                    }
                    String message = messageService.getMessage(finalCode, args);
                    e.sendMessage(message).queue(t -> consumer.accept(null), t -> consumer.accept(null));
                });
            }, t -> consumer.accept(null));
        } catch (Exception e) {
            consumer.accept(null);
        }
    }

    @Override
    protected Class<ModerationConfig> getDomainClass() {
        return ModerationConfig.class;
    }
}
