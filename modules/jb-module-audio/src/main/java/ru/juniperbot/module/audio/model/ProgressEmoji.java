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
package ru.juniperbot.module.audio.model;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum ProgressEmoji {

    START_SEGMENT(
            "<:pstart0:640553319143702569>",
            "<:pstart2:640553350999572506>",
            "<:pstart4:640553364152778752>",
            "<:pstart6:640553405366140979>",
            "<:pstart8:640553422919303168>",
            "<:pstart10:640553437263560715>"
    ),
    INTERMEDIATE_SEGMENT(
            "<:pintermediate0:640553241238568960>",
            "<:pintermediate2:640553249190969366>",
            "<:pintermediate4:640553260658196511>",
            "<:pintermediate6:640553281965522954>",
            "<:pintermediate8:640553294279999511>",
            "<:pintermediate10:640553309681352714>"
    ),
    END_SEGMENT(
            "<:pend0:640553153061978133>",
            "<:pend2:640553172276084786>",
            "<:pend4:640553186457026583>",
            "<:pend6:640553208787238912>",
            "<:pend8:640553220372037672>",
            "<:pend10:640553231814098978>"
    );

    private final String part0;
    private final String part2;
    private final String part4;
    private final String part6;
    private final String part8;
    private final String part10;

    public String getEmoji(int value) {
        if (value <= 0) {
            return part0;
        }
        if (value <= 2) {
            return part2;
        }
        if (value <= 4) {
            return part4;
        }
        if (value <= 6) {
            return part6;
        }
        if (value <= 8) {
            return part8;
        }
        return part10;
    }
}
