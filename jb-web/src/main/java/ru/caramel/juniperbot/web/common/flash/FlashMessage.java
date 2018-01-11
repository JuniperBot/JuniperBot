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
package ru.caramel.juniperbot.web.common.flash;

import lombok.Getter;
import org.springframework.context.MessageSourceResolvable;

import java.io.Serializable;

public class FlashMessage implements Serializable {

    private static final long serialVersionUID = -6146729966504725892L;

    @Getter
    private final String key;

    @Getter
    private final Flash.FlashType type;

    @Getter
    private final MessageSourceResolvable resolvable;

    public FlashMessage(Flash.FlashType type, String key, MessageSourceResolvable resolvable) {
        this.type = type;
        this.key = key;
        this.resolvable = resolvable;
    }
}
