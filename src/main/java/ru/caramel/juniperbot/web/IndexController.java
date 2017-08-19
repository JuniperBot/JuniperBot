package ru.caramel.juniperbot.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import ru.caramel.juniperbot.web.common.AbstractController;
import ru.caramel.juniperbot.web.common.navigation.Navigation;
import ru.caramel.juniperbot.web.common.navigation.PageElement;

@Controller
public class IndexController extends AbstractController {

    @Value("${discord.oauth.clientId}")
    private String clientId;

    @Value("${discord.oauth.permissions:0}")
    private String permissions;

    @RequestMapping("/")
    @Navigation(PageElement.HOME)
    public ModelAndView home() {
        return new ModelAndView("index")
                .addObject("clientId", clientId)
                .addObject("permissions", permissions);
    }
}
