package ru.caramel.juniperbot.web.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.acls.model.NotFoundException;
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

    protected void validateGuildId(long id) {
        DiscordGuildDetails details = tokenServices.getGuildById(id);
        if (details == null) {
            throw new NotFoundException("No guild found for user");
        }
        if (!tokenServices.hasPermission(details)) {
            throw new AccessDeniedException("User is not owner of this guild");
        }
    }
}
