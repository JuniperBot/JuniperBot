package ru.caramel.juniperbot.web.dashboard.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class DashboardController {

    @RequestMapping("/dashboard/{serverId}")
    public ModelAndView dashboard(@PathVariable long serverId) {
        ModelAndView mv = new ModelAndView("dashboard");
        mv.addObject("serverId", serverId);
        return mv;
    }
}
