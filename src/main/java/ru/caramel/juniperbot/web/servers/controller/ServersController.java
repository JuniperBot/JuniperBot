package ru.caramel.juniperbot.web.servers.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ServersController {

    @RequestMapping("/servers")
    public ModelAndView next() {
        ModelAndView mv = new ModelAndView("servers");
        return mv;
    }
}
