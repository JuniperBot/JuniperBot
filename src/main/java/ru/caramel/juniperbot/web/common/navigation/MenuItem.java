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
