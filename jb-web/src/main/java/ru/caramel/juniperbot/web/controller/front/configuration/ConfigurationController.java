/*
 * This file is part of JuniperBotJ.
 *
 * JuniperBotJ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * JuniperBotJ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with JuniperBotJ. If not, see <http://www.gnu.org/licenses/>.
 */
package ru.caramel.juniperbot.web.controller.front.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import ru.caramel.juniperbot.core.service.ContextService;
import ru.caramel.juniperbot.web.common.navigation.Navigation;
import ru.caramel.juniperbot.web.common.navigation.PageElement;
import ru.caramel.juniperbot.web.common.validation.ConfigValidator;
import ru.caramel.juniperbot.web.controller.front.AbstractController;
import ru.caramel.juniperbot.web.dao.ConfigDao;
import ru.caramel.juniperbot.web.dto.ConfigDto;

@Controller
@Navigation(PageElement.CONFIG_COMMON)
public class ConfigurationController extends AbstractController {

    @Autowired
    private ConfigDao configDao;

    @Autowired
    private ConfigValidator validator;

    @Autowired
    private ContextService contextService;

    @InitBinder
    public void init(WebDataBinder binder) {
        binder.setValidator(validator);
    }

    @RequestMapping("/config/{serverId}")
    public ModelAndView view(@PathVariable long serverId) {
        validateGuildId(serverId);
        return createModel("config", serverId)
                .addObject("config", configDao.getConfig(serverId));
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
        configDao.saveConfig(config, serverId);
        flash.success("flash.config.save.success.message");
        return view(serverId);
    }

    @Override
    protected ModelAndView createModel(String name, long serverId) {
        return super.createModel(name, serverId)
                .addObject("voiceChannels", getVoiceChannels(serverId))
                .addObject("textChannels", getTextChannels(serverId))
                .addObject("locales", contextService.getSupportedLocales().keySet());
    }



}
