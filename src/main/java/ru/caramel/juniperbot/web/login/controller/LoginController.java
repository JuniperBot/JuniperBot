package ru.caramel.juniperbot.web.login.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller("/login")
public class LoginController {

    @RequestMapping
    public ModelAndView get() {
        return new ModelAndView("login");
    }
}
