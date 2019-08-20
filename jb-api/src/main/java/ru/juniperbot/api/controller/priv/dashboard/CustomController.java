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
package ru.juniperbot.api.controller.priv.dashboard;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import ru.juniperbot.api.common.aspect.GuildId;
import ru.juniperbot.api.common.validation.CommandsContainerValidator;
import ru.juniperbot.api.controller.base.BaseRestController;
import ru.juniperbot.api.dao.CustomCommandsDao;
import ru.juniperbot.api.dto.config.CustomCommandDto;
import ru.juniperbot.api.dto.config.CustomCommandsContainerDto;
import ru.juniperbot.common.model.command.CommandInfo;
import ru.juniperbot.common.model.exception.NotFoundException;

import java.util.List;
import java.util.stream.Collectors;

@RestController
public class CustomController extends BaseRestController {

    @Autowired
    private CustomCommandsDao commandsDao;

    @Autowired
    private CommandsContainerValidator validator;

    @InitBinder
    public void init(WebDataBinder binder) {
        binder.setValidator(validator);
    }

    @RequestMapping(value = "/custom/{guildId}", method = RequestMethod.GET)
    @ResponseBody
    public CustomCommandsContainerDto load(@GuildId @PathVariable long guildId) {
        CustomCommandsContainerDto container = new CustomCommandsContainerDto();
        container.setCommands(commandsDao.get(guildId));
        List<CommandInfo> commandInfoList = gatewayService.getCommandList();
        container.setReservedKeys(commandInfoList.stream().map(CommandInfo::getKey).collect(Collectors.toSet()));
        return container;
    }

    @RequestMapping(value = "/custom/{guildId}", method = RequestMethod.POST)
    public void save(@GuildId @PathVariable long guildId,
                     @RequestBody @Validated List<CustomCommandDto> dtos) {
        commandsDao.save(dtos, guildId);
    }

    @RequestMapping(value = "/custom/{guildId}/config", method = RequestMethod.POST)
    public void save(@GuildId @PathVariable long guildId,
                     @RequestBody CustomCommandDto commandDto) {
        if (!commandsDao.saveConfig(commandDto, guildId)) {
            throw new NotFoundException();
        }
    }
}
