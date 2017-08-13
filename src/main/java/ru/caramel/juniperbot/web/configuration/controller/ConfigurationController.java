package ru.caramel.juniperbot.web.configuration.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ConfigurationController {

    @RequestMapping("/config")
    public ModelAndView next() {
        ModelAndView mv = new ModelAndView("config");
        return mv;
    }
}
