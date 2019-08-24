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
package ru.juniperbot.common.model;

import java.util.function.BiFunction;

public enum AvatarType {
    AVATAR((id, iconId) -> iconId == null ? null : String.format("https://cdn.discordapp.com/avatars/%s/%s.%s", id, iconId, iconId.startsWith("a_") ? "gif" : "png")),
    ICON((id, iconId) -> iconId == null ? null : String.format("https://cdn.discordapp.com/icons/%s/%s.%s", id, iconId, iconId.startsWith("a_") ? "gif" : "png")),
    SPLASH((id, iconId) -> iconId == null ? null : String.format("https://cdn.discordapp.com/splashes/%s/%s.png", id, iconId)),
    BANNER((id, iconId) -> iconId == null ? null : String.format("https://cdn.discordapp.com/banners/%s/%s.png", id, iconId));

    private final BiFunction<String, String, String> formatter;

    AvatarType(BiFunction<String, String, String> formatter) {
        this.formatter = formatter;
    }

    public String getUrl(String id, String iconId) {
        return formatter.apply(id, iconId);
    }
}
