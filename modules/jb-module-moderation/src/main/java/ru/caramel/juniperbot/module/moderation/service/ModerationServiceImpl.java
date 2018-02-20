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
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.caramel.juniperbot.core.persistence.entity.GuildConfig;
import ru.caramel.juniperbot.core.persistence.entity.LocalMember;
import ru.caramel.juniperbot.core.service.ConfigService;
import ru.caramel.juniperbot.core.service.MemberService;
import ru.caramel.juniperbot.core.service.MessageService;
import ru.caramel.juniperbot.core.utils.CommonUtils;
import ru.caramel.juniperbot.module.moderation.model.SlowMode;
import ru.caramel.juniperbot.module.moderation.persistence.entity.MemberWarning;
import ru.caramel.juniperbot.module.moderation.persistence.entity.ModerationConfig;
import ru.caramel.juniperbot.module.moderation.persistence.repository.MemberWarningRepository;
import ru.caramel.juniperbot.module.moderation.persistence.repository.ModerationConfigRepository;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class ModerationServiceImpl implements ModerationService {

    private final static String MUTED_ROLE_NAME = "JB-MUTED";

    private final static String COLOR_ROLE_NAME = "JB-CLR-";

    @Autowired
    private ModerationConfigRepository configRepository;

    @Autowired
    private MemberWarningRepository warningRepository;

    @Autowired
    private ConfigService configService;

    @Autowired
    private MemberService memberService;

    @Autowired
    private MessageService messageService;

    private Map<Long, SlowMode> slowModeMap = new ConcurrentHashMap<>();

    @Transactional
    @Override
    public ModerationConfig getConfig(Guild guild) {
        return getConfig(guild.getIdLong());
    }

    @Transactional
    @Override
    public ModerationConfig getConfig(long serverId) {
        ModerationConfig config = configRepository.findByGuildId(serverId);
        if (config == null) {
            GuildConfig guildConfig = configService.getOrCreate(serverId);
            config = new ModerationConfig();
            config.setGuildConfig(guildConfig);
            configRepository.save(config);
        }
        return config;
    }

    @Transactional
    @Override
    public ModerationConfig save(ModerationConfig config) {
        return configRepository.save(config);
    }

    @Override
    public boolean isModerator(Member member) {
        if (member == null) {
            return false;
        }
        if (member.hasPermission(Permission.ADMINISTRATOR) || member.isOwner()) {
            return true;
        }
        ModerationConfig config = getConfig(member.getGuild());
        return CollectionUtils.isNotEmpty(config.getRoles())
                && member.getRoles().stream().anyMatch(e -> config.getRoles().contains(e.getIdLong()));
    }

    @Override
    public boolean isPublicColor(long serverId) {
        ModerationConfig config = getConfig(serverId);
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
                        override.getManagerUpdatable().deny(permission).update().queue();
                    }
                    break;
                case UNCHECKED:
                case ALLOW:
                    if (!override.getAllowed().contains(permission)) {
                        override.getManagerUpdatable().clear(permission).update().queue();
                    }
                    break;
            }
        }
    }

    @Override
    public boolean mute(TextChannel channel, Member member, boolean global) {
        if (global) {
            Role mutedRole = getMutedRole(channel.getGuild());
            if (!member.getRoles().contains(mutedRole)) {
                channel.getGuild()
                        .getController()
                        .addRolesToMember(member, mutedRole)
                        .queue();
                member.getGuild().getController().setMute(member, true).queue();
                return true;
            }
        } else {
            PermissionOverride override = channel.getPermissionOverride(member);
            if (override != null && !override.getDenied().contains(Permission.MESSAGE_WRITE)) {
                override.getManagerUpdatable().deny(Permission.MESSAGE_WRITE).update().queue();
                return true;
            }
            if (override == null) {
                channel.createPermissionOverride(member)
                        .setDeny(Permission.MESSAGE_WRITE)
                        .queue();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean unmute(TextChannel channel, Member member) {
        boolean result = false;
        Role mutedRole = getMutedRole(channel.getGuild());
        if (member.getRoles().contains(mutedRole)) {
            channel.getGuild()
                    .getController()
                    .removeRolesFromMember(member, mutedRole)
                    .queue();
            member.getGuild().getController().setMute(member, false).queue();
            result = true;
        }
        PermissionOverride override = channel.getPermissionOverride(member);
        if (override != null) {
            override.delete().queue();
            result |= true;
        }
        return result;
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
        if (member == null || isModerator(member) || member.getUser().isBot()) {
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
    public void kick(Member author, Member member) {
        kick(author, member, null);
    }

    @Override
    public void kick(Member author, Member member, String reason) {
        if (member.getGuild().getSelfMember().hasPermission(Permission.KICK_MEMBERS)) {
            reason = StringUtils.isNotEmpty(reason)
                    ? String.format("%s: %s", author.getEffectiveName(), reason)
                    : author.getEffectiveName();
            member.getGuild().getController().kick(member, reason).queue();
        }
    }

    @Override
    public void ban(Member author, Member member) {
        ban(author, member, null);
    }

    @Override
    public void ban(Member author, Member member, String reason) {
        ban(author, member, 0, reason);
    }

    @Override
    public void ban(Member author, Member member, int delDays, String reason) {
        if (member.getGuild().getSelfMember().hasPermission(Permission.BAN_MEMBERS)) {
            reason = StringUtils.isNotEmpty(reason)
                    ? String.format("%s: %s", author.getEffectiveName(), reason)
                    : author.getEffectiveName();
            member.getGuild().getController().ban(member, delDays, reason).queue();
        }
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
        GuildConfig config = configService.getOrCreate(member.getGuild());
        LocalMember memberLocal = memberService.getOrCreate(member);
        return warningRepository.countActiveByViolator(config, memberLocal);
    }

    @Override
    @Transactional
    public boolean warn(Member author, Member member, String reason) {
        GuildConfig config = configService.getOrCreate(member.getGuild());
        ModerationConfig moderationConfig = getConfig(member.getGuild());
        LocalMember authorLocal = memberService.getOrCreate(author);
        LocalMember memberLocal = memberService.getOrCreate(member);

        boolean exceed = warningRepository.countActiveByViolator(config, memberLocal) >= moderationConfig.getMaxWarnings() - 1;
        MemberWarning warning = new MemberWarning(config, authorLocal, memberLocal, reason);
        if (exceed) {
            warningRepository.flushWarnings(config, memberLocal);
            warning.setActive(false);
            ban(author, member, messageService.getMessage("discord.command.mod.warn.ban.reason"));
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
}
