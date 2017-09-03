package ru.caramel.juniperbot.web.common.navigation;

import lombok.Getter;

public enum PageElement {
    HOME("fa fa-home", "/"),
    SERVERS("fa fa-server", "/servers"),
    DASHBOARD("fa fa-dashboard", "/dashboard/${serverId}"),
    CONFIG("fa fa-cogs", null),
    CONFIG_COMMON("fa fa-cog", "/config/${serverId}"),
    CONFIG_COMMANDS("fa fa-exclamation", "/commands/${serverId}"),
    CONFIG_CUSTOM_COMMANDS("fa fa-terminal", "/custom-commands/${serverId}")
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
