package ru.caramel.juniperbot.web.dashboard.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class DashboardController {

    @RequestMapping("/dashboard")
    public ModelAndView next() {
        ModelAndView mv = new ModelAndView("dashboard");
        return mv;
    }
}
