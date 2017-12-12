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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import ru.caramel.juniperbot.model.enums.CommandGroup;
import ru.caramel.juniperbot.model.dto.CommandTypeDto;
import ru.caramel.juniperbot.model.dto.CommandsDto;
import ru.caramel.juniperbot.persistence.entity.GuildConfig;
import ru.caramel.juniperbot.service.CommandsHolderService;
import ru.caramel.juniperbot.service.CommandsService;
import ru.caramel.juniperbot.utils.ArrayUtil;
import ru.caramel.juniperbot.web.common.AbstractController;
import ru.caramel.juniperbot.web.common.navigation.Navigation;
import ru.caramel.juniperbot.web.common.navigation.PageElement;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@Navigation(PageElement.CONFIG_COMMANDS)
public class CommandsController extends AbstractController {

    @Autowired
    private CommandsService commandsService;

    @Autowired
    private CommandsHolderService holderService;

    @RequestMapping("/commands/{serverId}")
    public ModelAndView view(@PathVariable long serverId) {
        validateGuildId(serverId);
        GuildConfig config = configService.getOrCreate(serverId);
        CommandsDto dto = new CommandsDto(ArrayUtil.reverse(String[].class, config.getDisabledCommands(), holderService.getCommands().keySet()));
        return createModel("commands", serverId, config.getPrefix())
                .addObject("commandsContainer", dto);
    }

    @RequestMapping(value = "/commands/{serverId}", method = RequestMethod.POST)
    public ModelAndView save(
            @PathVariable long serverId,
            @ModelAttribute("commandsContainer") CommandsDto container) {
        validateGuildId(serverId);
        GuildConfig config = configService.getOrCreate(serverId);
        config.setDisabledCommands(ArrayUtil.reverse(String[].class, container.getCommands(), holderService.getCommands().keySet()));
        configService.save(config);
        flash.success("flash.commands.save.success.message");
        return view(serverId);
    }

    protected ModelAndView createModel(String model, long serverId) {
        return createModel(model, serverId, null);
    }

    protected ModelAndView createModel(String model, long serverId, String prefix) {
        Map<CommandGroup, List<CommandTypeDto>> descriptors = new LinkedHashMap<>();
        holderService.getDescriptors().forEach((group, descriptor) -> {
            if (CommandGroup.CUSTOM.equals(group)) return;
            descriptors.put(group, descriptor.stream().map(CommandTypeDto::new).collect(Collectors.toList()));
        });
        return super.createModel(model, serverId)
                .addObject("commandTypes", descriptors)
                .addObject("commandPrefix", prefix != null ? prefix : configService.getPrefix(serverId));
    }
}
