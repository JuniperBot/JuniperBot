package ru.caramel.juniperbot.web.configuration.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import ru.caramel.juniperbot.web.common.AbstractController;
import ru.caramel.juniperbot.web.common.navigation.Navigation;
import ru.caramel.juniperbot.web.common.navigation.PageElement;

@Controller
public class ConfigurationController extends AbstractController {

    @RequestMapping("/config/{serverId}")
    @Navigation(PageElement.CONFIG)
    public ModelAndView config(@PathVariable long serverId) {
        return createModel("config", serverId);
    }
}
