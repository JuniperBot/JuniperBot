package ru.caramel.juniperbot.web.servers.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import ru.caramel.juniperbot.security.auth.DiscordTokenServices;

@Controller
public class ServersController {

    @Autowired
    private DiscordTokenServices discordTokenServices;

    @RequestMapping("/servers")
    public ModelAndView servers() {
        ModelAndView mv = new ModelAndView("servers");
        mv.addObject("servers", discordTokenServices.getCurrentGuilds(true));
        return mv;
    }
}
