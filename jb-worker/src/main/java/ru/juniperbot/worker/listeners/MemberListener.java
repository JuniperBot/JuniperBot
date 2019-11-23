/*
 * This file is part of JuniperBot.
 *
 * JuniperBot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * JuniperBot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with JuniperBot. If not, see <http://www.gnu.org/licenses/>.
 */
package ru.juniperbot.worker.listeners;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.audit.ActionType;
import net.dv8tion.jda.api.audit.AuditLogEntry;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.GuildBanEvent;
import net.dv8tion.jda.api.events.guild.GuildUnbanEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberLeaveEvent;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateOnlineStatusEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import ru.juniperbot.common.model.AuditActionType;
import ru.juniperbot.common.model.ModerationActionType;
import ru.juniperbot.common.persistence.entity.LocalMember;
import ru.juniperbot.common.persistence.entity.LocalUser;
import ru.juniperbot.common.service.MemberService;
import ru.juniperbot.common.service.UserService;
import ru.juniperbot.common.worker.event.DiscordEvent;
import ru.juniperbot.common.worker.event.listeners.DiscordEventListener;
import ru.juniperbot.common.worker.modules.audit.model.AuditActionBuilder;
import ru.juniperbot.common.worker.modules.audit.provider.ModerationAuditForwardProvider;
import ru.juniperbot.common.worker.modules.audit.provider.NicknameChangeAuditForwardProvider;
import ru.juniperbot.common.worker.modules.audit.provider.VoiceMoveAuditForwardProvider;
import ru.juniperbot.common.worker.modules.audit.service.ActionsHolderService;
import ru.juniperbot.common.worker.modules.moderation.model.ModerationActionRequest;
import ru.juniperbot.common.worker.modules.moderation.service.ModerationService;
import ru.juniperbot.common.worker.modules.moderation.service.MuteService;

import javax.annotation.Nonnull;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static ru.juniperbot.common.worker.modules.audit.provider.ModerationAuditForwardProvider.DURATION_MS_ATTR;

@DiscordEvent(priority = 0)
public class MemberListener extends DiscordEventListener {

    @Autowired
    private MemberService memberService;

    @Autowired
    private UserService userService;

    @Autowired
    private MuteService muteService;

    @Autowired
    private ModerationService moderationService;

    @Autowired
    private ActionsHolderService actionsHolderService;

    private final Cache<String, OnlineStatus> statusCache = CacheBuilder.newBuilder()
            .concurrencyLevel(7)
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .build();

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        if (event.getMember().getUser().isBot()) {
            return;
        }
        LocalMember member = entityAccessor.getOrCreate(event.getMember());
        muteService.refreshMute(event.getMember());
        getAuditService().log(event.getGuild(), AuditActionType.MEMBER_JOIN)
                .withUser(member)
                .save();
    }

    @Override
    public void onGuildBan(GuildBanEvent event) {
        Guild guild = event.getGuild();
        if (event.getUser().isBot() || !guild.getSelfMember().hasPermission(Permission.BAN_MEMBERS)) {
            return;
        }
        guild.retrieveBan(event.getUser()).queueAfter(2, TimeUnit.SECONDS, e -> {
            if (guild.getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) {
                guild.retrieveAuditLogs()
                        .type(ActionType.BAN)
                        .limit(10)
                        .queue(a -> {
                            User moderatorUser = a.stream()
                                    .filter(u -> Objects.equals(u.getTargetId(), event.getUser().getId()))
                                    .map(AuditLogEntry::getUser)
                                    .findFirst()
                                    .orElse(null);
                            if (moderatorUser != null && moderatorUser.equals(guild.getSelfMember().getUser())) {
                                moderatorUser = null;
                            }
                            ModerationActionRequest lastAction = moderationService.getLastAction(guild, event.getUser());
                            if (lastAction != null && lastAction.getType() != ModerationActionType.BAN) {
                                lastAction = null; // not a ban
                            }

                            AuditActionBuilder builder = getAuditService().log(guild, AuditActionType.MEMBER_BAN)
                                    .withTargetUser(event.getUser())
                                    .withAttribute(ModerationAuditForwardProvider.REASON_ATTR, e.getReason());
                            LocalMember moderator = moderatorUser != null ? memberService.get(guild, moderatorUser) : null;
                            if (moderator != null) {
                                builder.withUser(moderator);
                            } else if (moderatorUser != null) {
                                builder.withUser(moderatorUser);
                            } else if (lastAction != null && lastAction.getModeratorId() != null) {
                                builder.withUser(guild.getMemberById(lastAction.getModeratorId()));
                            }
                            if (lastAction != null) {
                                builder.withAttribute(DURATION_MS_ATTR, lastAction.getDuration());
                            }
                            builder.save();
                        });
            } else {
                AuditActionBuilder builder = getAuditService().log(guild, AuditActionType.MEMBER_BAN)
                        .withTargetUser(event.getUser())
                        .withAttribute(ModerationAuditForwardProvider.REASON_ATTR, e.getReason());
                ModerationActionRequest lastAction = moderationService.getLastAction(guild, event.getUser());
                if (lastAction != null && lastAction.getType() == ModerationActionType.BAN) {
                    if (lastAction.getModeratorId() != null) {
                        builder.withUser(guild.getMemberById(lastAction.getModeratorId()));
                    }
                    builder.withAttribute(DURATION_MS_ATTR, lastAction.getDuration());
                }
                builder.save();
            }
        });
    }

    @Override
    public void onGuildUnban(GuildUnbanEvent event) {
        if (event.getUser().isBot() || !event.getGuild().getSelfMember().hasPermission(Permission.BAN_MEMBERS)) {
            return;
        }
        AuditActionBuilder actionBuilder = getAuditService().log(event.getGuild(), AuditActionType.MEMBER_UNBAN);
        LocalMember member = memberService.get(event.getGuild(), event.getUser());
        if (member != null) {
            actionBuilder.withTargetUser(member);
        } else {
            actionBuilder.withTargetUser(event.getUser());
        }
        moderationService.removeUnBanSchedule(event.getGuild().getId(), event.getUser().getId());
        actionBuilder.save();
    }

    @Override
    @Transactional
    public void onGuildMemberLeave(GuildMemberLeaveEvent event) {
        if (event.getMember().getUser().isBot()) {
            return;
        }
        LocalMember member = entityAccessor.getOrCreate(event.getMember());
        member.setLastKnownRoles(event.getMember().getRoles().stream()
                .map(Role::getIdLong).collect(Collectors.toList()));
        memberService.save(member);

        if (!actionsHolderService.isLeaveNotified(event.getGuild().getIdLong(), event.getUser().getIdLong())) {
            getAuditService().log(event.getGuild(), AuditActionType.MEMBER_LEAVE)
                    .withUser(member)
                    .save();
        }
    }

    @Override
    @Transactional
    public void onGuildMemberUpdateNickname(GuildMemberUpdateNicknameEvent event) {
        LocalMember member = memberService.get(event.getMember());
        if (member != null && !Objects.equals(event.getMember().getEffectiveName(), member.getEffectiveName())) {
            getAuditService().log(event.getGuild(), AuditActionType.MEMBER_NAME_CHANGE)
                    .withUser(member)
                    .withAttribute(NicknameChangeAuditForwardProvider.OLD_NAME, member.getEffectiveName())
                    .withAttribute(NicknameChangeAuditForwardProvider.NEW_NAME, event.getMember().getEffectiveName())
                    .save();
            member.setEffectiveName(event.getMember().getEffectiveName());
            memberService.save(member);
        }
    }

    @Override
    @Transactional
    public void onGuildVoiceJoin(GuildVoiceJoinEvent event) {
        if (!event.getMember().getUser().isBot()) {
            getAuditService().log(event.getGuild(), AuditActionType.VOICE_JOIN)
                    .withUser(event.getMember())
                    .withChannel(event.getChannelJoined())
                    .save();
        }
    }

    @Override
    @Transactional
    public void onGuildVoiceMove(GuildVoiceMoveEvent event) {
        if (!event.getMember().getUser().isBot()) {
            getAuditService().log(event.getGuild(), AuditActionType.VOICE_MOVE)
                    .withUser(event.getMember())
                    .withChannel(event.getChannelJoined())
                    .withAttribute(VoiceMoveAuditForwardProvider.OLD_CHANNEL, event.getChannelLeft())
                    .save();
        }
    }

    @Override
    @Transactional
    public void onGuildVoiceLeave(GuildVoiceLeaveEvent event) {
        if (!event.getMember().getUser().isBot()) {
            getAuditService().log(event.getGuild(), AuditActionType.VOICE_LEAVE)
                    .withUser(event.getMember())
                    .withChannel(event.getChannelLeft())
                    .save();
        }
    }

    @Override
    @Transactional
    public void onUserUpdateOnlineStatus(@Nonnull UserUpdateOnlineStatusEvent event) {
        OnlineStatus newStatus = event.getNewOnlineStatus();
        OnlineStatus oldStatus = event.getOldOnlineStatus();
        if (event.getUser().isBot()
                || oldStatus == OnlineStatus.OFFLINE
                || oldStatus == OnlineStatus.INVISIBLE
                || (newStatus != OnlineStatus.OFFLINE && newStatus != OnlineStatus.INVISIBLE)) {
            return;
        }

        try {
            // this event is executed multiple times for each mutual guild, we want this only once at least per minute
            statusCache.get(event.getUser().getId(), () -> {
                LocalUser user = entityAccessor.getOrCreate(event.getUser());
                user.setLastOnline(new Date());
                userService.save(user);
                return newStatus;
            });
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}