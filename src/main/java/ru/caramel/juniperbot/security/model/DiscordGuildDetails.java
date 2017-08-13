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
