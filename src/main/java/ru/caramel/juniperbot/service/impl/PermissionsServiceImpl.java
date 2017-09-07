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
package ru.caramel.juniperbot.service.impl;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Channel;
import net.dv8tion.jda.core.entities.Guild;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.caramel.juniperbot.integration.discord.DiscordClient;
import ru.caramel.juniperbot.service.PermissionsService;

@Service
public class PermissionsServiceImpl implements PermissionsService {

    @Autowired
    private DiscordClient discordClient;

    @Override
    public boolean hasWebHooksAccess(Guild guild) {
        if (!discordClient.isConnected() || guild == null) {
            return false;
        }
        return checkPermission(guild, Permission.MANAGE_WEBHOOKS);
    }

    @Override
    public boolean hasWebHooksAccess(Channel channel) {
        if (!discordClient.isConnected() || channel == null) {
            return false;
        }
        return checkPermission(channel, Permission.MANAGE_WEBHOOKS);
    }

    @Override
    public boolean checkPermission(Guild guild, Permission permission) {
        if (!discordClient.isConnected()) {
            return false;
        }
        return guild.getSelfMember().hasPermission(permission);
    }

    @Override
    public boolean checkPermission(Channel channel, Permission permission) {
        if (!discordClient.isConnected()) {
            return false;
        }
        return channel.getGuild().getSelfMember().hasPermission(channel, permission);
    }
}
