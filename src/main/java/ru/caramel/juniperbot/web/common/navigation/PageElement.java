package ru.caramel.juniperbot.web.common.navigation;

import lombok.Getter;

public enum PageElement {
    HOME("Главная", "fa fa-home", "/"),
    SERVERS("Серверы (${serverName})", "fa fa-server", "/servers"),
    DASHBOARD("Мониторинг", "fa fa-dashboard", "/dashboard/${serverId}"),
    CONFIG("Конфигурация", "fa fa-cogs", "/config/${serverId}")
    ;

    @Getter
    private String name;

    @Getter
    private String icon;

    @Getter
    private String url;

    PageElement(String name, String icon, String url) {
        this.name = name;
        this.url = url;
        this.icon = icon;
    }

    @Override
    public String toString() {
        return name;
    }
}
