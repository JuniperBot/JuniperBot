package ru.caramel.juniperbot.service;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Channel;
import net.dv8tion.jda.core.entities.Guild;

public interface PermissionsService {

    boolean hasWebHooksAccess(Guild guild);

    boolean hasWebHooksAccess(Channel channel);

    boolean checkPermission(Guild guild, Permission permission);

    boolean checkPermission(Channel channel, Permission permission);
}
