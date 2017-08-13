package ru.caramel.juniperbot.web.configuration.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ConfigurationController {

    @RequestMapping("/config/{serverId}")
    public ModelAndView config(@PathVariable long serverId) {
        ModelAndView mv = new ModelAndView("config");
        mv.addObject("serverId", serverId);
        return mv;
    }
}
