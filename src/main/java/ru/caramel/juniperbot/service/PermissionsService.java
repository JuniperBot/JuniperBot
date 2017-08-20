package ru.caramel.juniperbot.service;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;

public interface PermissionsService {

    boolean hasWebHooksAccess(Guild guild);

    boolean checkPermission(Guild guild, Permission permission);
}
