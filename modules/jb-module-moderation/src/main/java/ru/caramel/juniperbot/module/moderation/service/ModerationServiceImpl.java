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
import net.dv8tion.jda.core.utils.PermissionUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.caramel.juniperbot.core.persistence.entity.GuildConfig;
import ru.caramel.juniperbot.core.service.ConfigService;
import ru.caramel.juniperbot.module.moderation.model.SlowMode;
import ru.caramel.juniperbot.module.moderation.persistence.entity.ModerationConfig;
import ru.caramel.juniperbot.module.moderation.persistence.repository.ModerationConfigRepository;

import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ModerationServiceImpl implements ModerationService {

    private final static String MUTED_ROLE_NAME = "JB-MUTED";

    @Autowired
    private ModerationConfigRepository configRepository;

    @Autowired
    private ConfigService configService;

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

    public Role getMutedRole(Guild guild) {
        List<Role> mutedRoles = guild.getRolesByName(MUTED_ROLE_NAME, true);
        Role role = CollectionUtils.isNotEmpty(mutedRoles) ? mutedRoles.get(0) : null;
        if (role == null || !PermissionUtil.canInteract(guild.getSelfMember(), role)) {
            role = guild.getController()
                    .createRole()
                    .setColor(Color.GRAY)
                    .setMentionable(false)
                    .setName(MUTED_ROLE_NAME)
                    .complete();
        }
        for (TextChannel channel : guild.getTextChannels()) {
            PermissionOverride override = channel.getPermissionOverride(role);
            if (override == null
                    || !override.getDenied().contains(Permission.MESSAGE_WRITE)) {
                channel.createPermissionOverride(role)
                        .setDeny(Permission.MESSAGE_WRITE)
                        .submit();
            }
        }

        for (VoiceChannel channel : guild.getVoiceChannels()) {
            PermissionOverride override = channel.getPermissionOverride(role);
            if (override == null || !override.getDenied().contains(Permission.VOICE_SPEAK)) {
                channel.createPermissionOverride(role)
                        .setDeny(Permission.VOICE_SPEAK)
                        .submit();
            }
        }
        return role;
    }

    @Override
    public boolean mute(TextChannel channel, Member member, boolean global) {
        if (global) {
            Role mutedRole = getMutedRole(channel.getGuild());
            if (!member.getRoles().contains(mutedRole)) {
                channel.getGuild()
                        .getController()
                        .addRolesToMember(member, mutedRole)
                        .submit();
                return true;
            }
        } else {
            PermissionOverride override = channel.getPermissionOverride(member);
            if (override != null && !override.getDenied().contains(Permission.MESSAGE_WRITE)) {
                override.getManagerUpdatable().deny(Permission.MESSAGE_WRITE).update().submit();
                return true;
            }
            if (override == null) {
                channel.createPermissionOverride(member)
                        .setDeny(Permission.MESSAGE_WRITE)
                        .submit();
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
                    .submit();
            result = true;
        }
        PermissionOverride override = channel.getPermissionOverride(member);
        if (override != null) {
            override.delete().submit();
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
        if (isModerator(member) || member.getUser().isBot()) {
            return false;
        }
        SlowMode slowMode = slowModeMap.get(channel.getIdLong());
        return slowMode != null && slowMode.tick(member.getUser().getId());
    }

    @Override
    public boolean slowOff(TextChannel channel) {
        return slowModeMap.remove(channel.getIdLong()) != null;
    }
}
