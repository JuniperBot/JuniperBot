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
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class MenuItem implements Serializable {

    private static final long serialVersionUID = 3626205728803177946L;

    @Getter
    private final String icon;

    @Getter
    @Setter
    private String name;

    @Getter
    @Setter
    private String url;

    @Getter
    @Setter
    private MenuItem parent;

    @Getter
    @Setter
    private List<MenuItem> childs = new ArrayList<>();

    @Getter
    @Setter
    private PageElement element;

    @Setter
    private boolean active;

    @Getter
    @Setter
    private boolean current;

    public MenuItem(PageElement element) {
        this(element, false);
    }

    public MenuItem(PageElement element, boolean active) {
        this(element.getUrl(), element.toString(), element.getIcon(), active);
        this.element = element;
    }

    public MenuItem(String url, String name, String icon) {
        this(url, name, icon, false);
    }

    public MenuItem(String url, String name, String icon, boolean active) {
        this.url = url;
        this.name = name;
        this.icon = icon;
        this.active = active;
    }

    public boolean addChild(MenuItem child) {
        return childs.add(child);
    }

    public boolean isActive() {
        return active || childs.stream().anyMatch(MenuItem::isActive);
    }

}
