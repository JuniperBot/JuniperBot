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
package ru.juniperbot.worker.listeners;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.audit.ActionType;
import net.dv8tion.jda.api.audit.AuditLogEntry;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import ru.juniperbot.common.model.AuditActionType;
import ru.juniperbot.common.persistence.entity.LocalMember;
import ru.juniperbot.common.service.MemberService;
import ru.juniperbot.common.worker.event.DiscordEvent;
import ru.juniperbot.common.worker.event.listeners.DiscordEventListener;
import ru.juniperbot.common.worker.modules.audit.model.AuditActionBuilder;
import ru.juniperbot.common.worker.modules.audit.provider.ModerationAuditForwardProvider;
import ru.juniperbot.common.worker.modules.audit.provider.NicknameChangeAuditForwardProvider;
import ru.juniperbot.common.worker.modules.audit.service.ActionsHolderService;
import ru.juniperbot.common.worker.modules.moderation.service.ModerationService;
import ru.juniperbot.common.worker.modules.moderation.service.MuteService;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@DiscordEvent(priority = 0)
public class MemberListener extends DiscordEventListener {

    @Autowired
    private MemberService memberService;

    @Autowired
    private MuteService muteService;

    @Autowired
    private ModerationService moderationService;

    @Autowired
    private ActionsHolderService actionsHolderService;

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

                            AuditActionBuilder builder = getAuditService().log(guild, AuditActionType.MEMBER_BAN)
                                    .withTargetUser(event.getUser())
                                    .withAttribute(ModerationAuditForwardProvider.REASON_ATTR, e.getReason());
                            LocalMember moderator = moderatorUser != null ? memberService.get(guild, moderatorUser) : null;
                            if (moderator != null) {
                                builder.withUser(moderator);
                            } else if (moderatorUser != null) {
                                builder.withUser(moderatorUser);
                            } else {
                                Member lastModerator = moderationService.getLastActionModerator(guild, event.getUser());
                                builder.withUser(lastModerator);
                            }
                            builder.save();
                        });
            } else {
                Member lastModerator = moderationService.getLastActionModerator(guild, event.getUser());
                getAuditService().log(guild, AuditActionType.MEMBER_BAN)
                        .withUser(lastModerator)
                        .withTargetUser(event.getUser())
                        .withAttribute(ModerationAuditForwardProvider.REASON_ATTR, e.getReason())
                        .save();
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
        LocalMember member = entityAccessor.getOrCreate(event.getMember());
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
            getAuditService().log(event.getGuild(), AuditActionType.VOICE_JOIN)
                    .withUser(event.getMember())
                    .withChannel(event.getChannelJoined())
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
}