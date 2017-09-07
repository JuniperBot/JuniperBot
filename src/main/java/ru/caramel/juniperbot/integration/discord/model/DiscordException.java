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

public class DiscordException extends Exception {

    private static final long serialVersionUID = 4621225391411561750L;

    @Getter
    private final Object[] args;

    public DiscordException() {
        args = null;
    }

    public DiscordException(String message, Object... args) {
        this(message, null, args);
    }

    public DiscordException(String message, Throwable cause, Object... args) {
        super(message, cause);
        this.args = args;
    }
}
