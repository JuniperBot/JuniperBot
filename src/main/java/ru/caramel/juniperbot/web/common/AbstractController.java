package ru.caramel.juniperbot.web.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.ModelAndView;
import ru.caramel.juniperbot.security.auth.DiscordTokenServices;
import ru.caramel.juniperbot.security.model.DiscordGuildDetails;
import ru.caramel.juniperbot.web.common.flash.Flash;

public abstract class AbstractController {

    @Autowired
    protected Flash flash;

    @Autowired
    protected DiscordTokenServices tokenServices;

    protected ModelAndView createModel(String modelName, long serverId) {
        ModelAndView mv = new ModelAndView(modelName);
        mv.addObject("serverId", serverId);
        DiscordGuildDetails details = tokenServices.getGuildById(serverId);
        if (details != null) {
            mv.addObject("serverName", details.getName());
        }
        return mv;
    }
}
