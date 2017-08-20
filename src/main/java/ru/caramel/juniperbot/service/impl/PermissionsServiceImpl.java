package ru.caramel.juniperbot.service.impl;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.caramel.juniperbot.integration.discord.DiscordClient;
import ru.caramel.juniperbot.service.PermissionsService;

import java.util.List;

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
    public boolean checkPermission(Guild guild, Permission permission) {
        if (!discordClient.isConnected()) {
            return false;
        }
        Member member = guild.getMember(guild.getJDA().getSelfUser());
        List<Permission> permissionList = member.getPermissions();
        return permissionList.contains(Permission.ADMINISTRATOR) || permissionList.contains(permission);
    }
}
