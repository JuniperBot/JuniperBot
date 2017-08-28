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
