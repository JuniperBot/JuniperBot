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
package ru.caramel.juniperbot.web.common.navigation;

import lombok.Getter;

public enum PageElement {
    HOME("fa fa-home", "/"),
    CONFIG_COMMANDS("fa fa-exclamation", "/commands/${serverId}"),
    CONFIG_CUSTOM_COMMANDS("fa fa-terminal", "/custom-commands/${serverId}"),
    APIDOCS("fa fa-book", "/apidocs"),
    PATREON("fa fa-heart", "https://www.patreon.com/JuniperBot"),
    STATUS("fa fa-area-chart", "/status"),
    FUNNY("fa fa-futbol-o", "/funny/${serverId}"),
    ;

    @Getter
    private String icon;

    @Getter
    private String url;

    PageElement(String icon, String url) {
        this.url = url;
        this.icon = icon;
    }

    @Override
    public String toString() {
        return getClass().getName() + "." + name();
    }
}
