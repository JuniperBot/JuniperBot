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
package ru.caramel.juniperbot.core.model.enums;

import lombok.Getter;

import java.awt.*;

public enum AuditActionType {
    BOT_ADD,
    BOT_LEAVE,
    MEMBER_JOIN("#7DE848"),
    MEMBER_NAME_CHANGE("#7F9BFF"),
    MEMBER_LEAVE("#EAD967"),
    MEMBER_WARN("#FFCA59"),
    MEMBER_BAN("#FF686B"),
    MEMBER_KICK("#FFA154"),
    MEMBER_MUTE("#FFCA59"),
    MEMBER_UNMUTE("#85EA8A"),
    MESSAGE_DELETE("#FF6D96"),
    MESSAGE_EDIT("#60AFFF"),
    VOICE_JOIN("#AD84E8"),
    VOICE_LEAVE("#E5ACA0");

    @Getter
    private final Color color;

    AuditActionType() {
        this(null);
    }

    AuditActionType(String hex) {
        this.color = hex != null ? Color.decode(hex) : null;
    }
}