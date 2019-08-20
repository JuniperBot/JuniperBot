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

public enum MafiaState {
    NIGHT_COP("mafia.cop.transition.start", null),
    NIGHT_DOCTOR("mafia.doctor.transition.start", "mafia.doctor.transition.end"),
    NIGHT_BROKER("mafia.broker.transition.start", "mafia.broker.transition.end"),
    NIGHT_GOON(null, "mafia.goon.transition.end"),
    DAY,
    MEETING,
    CHOOSING,
    FINISH;

    @Getter
    private final String transitionStart;

    @Getter
    private final String transitionEnd;

    MafiaState() {
        this(null, null);
    }

    MafiaState(String transitionStart, String transitionEnd) {
        this.transitionStart = transitionStart;
        this.transitionEnd = transitionEnd;
    }
}
