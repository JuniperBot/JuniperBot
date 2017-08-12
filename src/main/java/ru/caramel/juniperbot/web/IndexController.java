package ru.caramel.juniperbot.web;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

@Controller
public class IndexController {
    @RequestMapping("/")
    public ModelAndView home() {
        ModelAndView mv = new ModelAndView("index");
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        mv.addObject("message", auth.getName());
        return mv;
    }

    @RequestMapping("/next")
    public ModelAndView next(Map<String, Object> model) {
        ModelAndView mv = new ModelAndView("next");
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        mv.addObject("message", auth.getName());
        return mv;
    }
}
