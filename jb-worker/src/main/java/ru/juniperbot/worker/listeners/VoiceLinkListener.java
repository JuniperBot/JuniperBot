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

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.guild.voice.GenericGuildVoiceEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import ru.juniperbot.common.persistence.entity.GuildConfig;
import ru.juniperbot.common.service.ConfigService;
import ru.juniperbot.common.worker.event.DiscordEvent;
import ru.juniperbot.common.worker.event.listeners.DiscordEventListener;

import java.util.Collections;
import java.util.Objects;

@DiscordEvent(priority = 0)
public class VoiceLinkListener extends DiscordEventListener {

    @Autowired
    private ConfigService configService;

    @Override
    public void onGuildVoiceJoin(GuildVoiceJoinEvent event) {
        GuildConfig config = getApplicableConfig(event);
        if (config == null) {
            return;
        }
        Role role = getRole(config, event.getChannelJoined());
        if (role != null) {
            event.getGuild().addRoleToMember(event.getMember(), role).queue();
        }
    }

    @Override
    public void onGuildVoiceMove(GuildVoiceMoveEvent event) {
        GuildConfig config = getApplicableConfig(event);
        if (config == null) {
            return;
        }
        Guild guild = event.getGuild();
        Member member = event.getMember();
        Role roleToAdd = getRole(config, event.getChannelJoined());
        Role roleToRemove = getRole(config, event.getChannelLeft());

        if (Objects.equals(roleToAdd, roleToRemove)) {
            return;
        }
        if (roleToAdd != null && roleToRemove != null) {
            guild.modifyMemberRoles(member, Collections.singleton(roleToAdd), Collections.singleton(roleToRemove)).queue();
        } else if (roleToAdd != null) {
            guild.addRoleToMember(member, roleToAdd).queue();
        } else if (roleToRemove != null) {
            guild.removeRoleFromMember(member, roleToRemove).queue();
        }
    }

    @Override
    public void onGuildVoiceLeave(GuildVoiceLeaveEvent event) {
        GuildConfig config = getApplicableConfig(event);
        if (config == null) {
            return;
        }
        Role role = getRole(config, event.getChannelLeft());
        if (role != null) {
            event.getGuild().removeRoleFromMember(event.getMember(), role).queue();
        }
    }

    private Role getRole(GuildConfig config, VoiceChannel channel) {
        if (channel == null) {
            return null;
        }
        Guild guild = channel.getGuild();
        return config.getVoiceLinks()
                .stream()
                .filter(e -> channel.getId().equals(e.getChannelId()))
                .findFirst()
                .map(e -> guild.getRoleById(e.getRoleId()))
                .filter(e -> guild.getSelfMember().canInteract(e))
                .orElse(null);
    }

    private GuildConfig getApplicableConfig(GenericGuildVoiceEvent event) {
        Guild guild = event.getGuild();
        if (event.getMember().getUser().isBot() || !guild.getSelfMember().hasPermission(Permission.MANAGE_ROLES)) {
            return null;
        }
        GuildConfig config = configService.get(guild);
        if (config == null || CollectionUtils.isEmpty(config.getVoiceLinks())) {
            return null;
        }
        return config;
    }
}