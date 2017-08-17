package ru.caramel.juniperbot.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import ru.caramel.juniperbot.web.common.AbstractController;
import ru.caramel.juniperbot.web.common.navigation.Navigation;
import ru.caramel.juniperbot.web.common.navigation.PageElement;

@Controller
public class IndexController extends AbstractController {
    @RequestMapping("/")
    @Navigation(PageElement.HOME)
    public ModelAndView home() {
        return new ModelAndView("index");
    }
}
