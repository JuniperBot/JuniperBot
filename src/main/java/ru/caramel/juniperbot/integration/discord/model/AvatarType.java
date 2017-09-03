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
