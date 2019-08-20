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
package ru.juniperbot.module.mafia.model;

import lombok.Getter;

public enum MafiaRole {
    TOWNIE,
    GOON(true),

    DOCTOR("mafia.doctor.choice"),
    COP("mafia.cop.choice"),
    BROKER(true, "mafia.broker.choice");

    @Getter
    private final boolean mafia;

    @Getter
    private final String choiceMessage;

    MafiaRole() {
        this(false);
    }

    MafiaRole(boolean mafia) {
        this(mafia, null);
    }

    MafiaRole(String choiceMessage) {
        this(false, choiceMessage);
    }

    MafiaRole(boolean mafia, String choiceMessage) {
        this.mafia = mafia;
        this.choiceMessage = choiceMessage;
    }
}
