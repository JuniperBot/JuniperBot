package ru.caramel.juniperbot.web.common.navigation;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static ru.caramel.juniperbot.web.common.navigation.PageElement.*;

@Component
public class MenuBuilder {

    public List<MenuItem> build() {
        List<MenuItem> items = new ArrayList<>();
        items.add(new MenuItem(DASHBOARD));
        MenuItem config = new MenuItem(CONFIG);
        config.addChild(new MenuItem(CONFIG_COMMON));
        config.addChild(new MenuItem(CONFIG_COMMANDS));
        items.add(config);
        return items;
    }

}
