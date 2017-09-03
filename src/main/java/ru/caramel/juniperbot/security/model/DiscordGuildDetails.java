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
package ru.caramel.juniperbot.security.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.core.Permission;
import ru.caramel.juniperbot.integration.discord.model.AvatarType;

import java.util.List;
import java.util.Map;

public class DiscordGuildDetails extends AbstractDetails {

    private static final long serialVersionUID = 2702379675490663478L;

    @Getter
    @Setter(AccessLevel.PRIVATE)
    private String name;

    @Getter
    @Setter(AccessLevel.PRIVATE)
    private String icon;

    @Getter
    @Setter(AccessLevel.PRIVATE)
    private boolean owner;

    @Getter
    @Setter(AccessLevel.PRIVATE)
    private List<Permission> permissions;

    public String getAvatarUrl() {
        return AvatarType.ICON.getUrl(id, icon);
    }

    public static DiscordGuildDetails create(Map<Object, Object> map) {
        DiscordGuildDetails details = new DiscordGuildDetails();
        setValue(String.class, map, "id", details::setId);
        setValue(String.class, map, "name", details::setName);
        setValue(String.class, map, "icon", details::setIcon);
        setValue(Boolean.class, map, "owner", details::setOwner);
        Object permissions = map.get("permissions");
        if (permissions instanceof Number) {
            details.permissions = Permission.getPermissions(((Number) permissions).longValue());
        }
        return details;
    }
}
