package ru.caramel.juniperbot.integration.discord.model;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

public enum AvatarType {
    USER("https://cdn.discordapp.com/avatars/%s/%s.jpg"),
    ICON("https://cdn.discordapp.com/icons/%s/%s.jpg"),
    NO_AVATAR("/resources/img/noavatar.png");

    @Getter
    private String format;

    AvatarType(String format) {
        this.format = format;
    }

    public String getUrl(String id, String avatar) {
        if (StringUtils.isNotEmpty(id) && StringUtils.isNotEmpty(avatar)) {
            return String.format(format, id, avatar);
        }
        return NO_AVATAR.format;
    }
}
