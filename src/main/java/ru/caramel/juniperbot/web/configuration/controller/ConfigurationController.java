package ru.caramel.juniperbot.web.configuration.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import ru.caramel.juniperbot.model.ConfigDto;
import ru.caramel.juniperbot.service.ConfigService;
import ru.caramel.juniperbot.web.common.AbstractController;
import ru.caramel.juniperbot.web.common.navigation.Navigation;
import ru.caramel.juniperbot.web.common.navigation.PageElement;

@Controller
@Navigation(PageElement.CONFIG)
public class ConfigurationController extends AbstractController {

    @Autowired
    private ConfigService configService;

    @RequestMapping("/config/{serverId}")
    public ModelAndView view(@PathVariable long serverId) {
        validateGuildId(serverId);
        return createModel("config", serverId)
                .addObject("config", configService.getConfig(serverId));
    }

    @RequestMapping(value = "/config/{serverId}", method = RequestMethod.POST)
    public ModelAndView save(
            @PathVariable long serverId,
            @Validated @ModelAttribute("config") ConfigDto config,
            BindingResult result) {
        validateGuildId(serverId);
        if (result.hasErrors()) {
            return createModel("config", serverId);
        }
        configService.saveConfig(config, serverId);
        flash.success("flash.config.save.success.message");
        return view(serverId);
    }

    @Override
    protected ModelAndView createModel(String name, long serverId) {
        return super.createModel(name, serverId)
                .addObject("voiceChannels", getVoiceChannels(serverId))
                .addObject("textChannels", getTextChannels(serverId));
    }
}
