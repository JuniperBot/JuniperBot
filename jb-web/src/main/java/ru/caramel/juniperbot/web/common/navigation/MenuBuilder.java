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

import org.springframework.stereotype.Component;
import ru.caramel.juniperbot.web.security.utils.SecurityUtils;

import java.util.ArrayList;
import java.util.List;

import static ru.caramel.juniperbot.web.common.navigation.PageElement.*;

@Component
public class MenuBuilder {

    public List<MenuItem> build() {
        List<MenuItem> items = new ArrayList<>();
        if (SecurityUtils.isAuthenticated()) {
            items.add(new MenuItem(CONFIG_COMMON));
            items.add(new MenuItem(RANKING));
            items.add(new MenuItem(WELCOME_MESSAGES));
            items.add(new MenuItem(CONFIG_COMMANDS));
            items.add(new MenuItem(CONFIG_CUSTOM_COMMANDS));
        }
        items.add(new MenuItem(STATUS, false, true));
        items.add(new MenuItem(APIDOCS, false, true));
        return items;
    }
}
