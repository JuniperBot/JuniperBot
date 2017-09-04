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
package ru.caramel.juniperbot.web.configuration.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import ru.caramel.juniperbot.model.enums.CommandType;
import ru.caramel.juniperbot.model.CommandsContainer;
import ru.caramel.juniperbot.persistence.entity.GuildConfig;
import ru.caramel.juniperbot.service.CommandsService;
import ru.caramel.juniperbot.service.MapperService;
import ru.caramel.juniperbot.web.common.AbstractController;
import ru.caramel.juniperbot.web.common.navigation.Navigation;
import ru.caramel.juniperbot.web.common.navigation.PageElement;
import ru.caramel.juniperbot.web.common.validation.CommandsContainerValidator;

@Controller
@Navigation(PageElement.CONFIG_CUSTOM_COMMANDS)
public class CustomCommandsController extends AbstractController {

    @Autowired
    private CommandsService commandsService;

    @Autowired
    private CommandsContainerValidator validator;

    @Autowired
    private MapperService mapperService;

    @InitBinder
    public void init(WebDataBinder binder) {
        binder.setValidator(validator);
    }

    @RequestMapping("/custom-commands/{serverId}")
    public ModelAndView view(@PathVariable long serverId) {
        validateGuildId(serverId);
        GuildConfig config = configService.getOrCreate(serverId, GuildConfig.COMMANDS_GRAPH);
        CommandsContainer container = new CommandsContainer(mapperService.getCommandsDto(config.getCommands()));
        return createModel("custom-commands", serverId, config.getPrefix())
                .addObject("commandsContainer", container);
    }

    @RequestMapping(value = "/custom-commands/{serverId}", method = RequestMethod.POST)
    public ModelAndView save(
            @PathVariable long serverId,
            @Validated @ModelAttribute("commandsContainer") CommandsContainer container,
            BindingResult result) {
        validateGuildId(serverId);
        if (result.hasErrors()) {
            return createModel("custom-commands", serverId);
        }
        commandsService.saveCommands(container.getCommands(), serverId);
        flash.success("flash.custom-commands.save.success.message");
        return view(serverId);
    }

    protected ModelAndView createModel(String model, long serverId) {
        return createModel(model, serverId, null);
    }

    private ModelAndView createModel(String model, long serverId, String prefix) {
        return super.createModel(model, serverId)
                .addObject("commandTypes", CommandType.values())
                .addObject("commandPrefix", prefix != null ? prefix : configService.getPrefix(serverId));
    }
}
