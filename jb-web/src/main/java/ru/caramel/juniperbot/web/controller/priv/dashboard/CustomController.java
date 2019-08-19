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
package ru.caramel.juniperbot.web.controller.priv.dashboard;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import ru.juniperbot.worker.common.command.service.CommandsHolderService;
import ru.juniperbot.common.model.exception.NotFoundException;
import ru.caramel.juniperbot.web.common.aspect.GuildId;
import ru.caramel.juniperbot.web.common.validation.CommandsContainerValidator;
import ru.caramel.juniperbot.web.controller.base.BaseRestController;
import ru.caramel.juniperbot.web.dao.CustomCommandsDao;
import ru.caramel.juniperbot.web.dto.config.CustomCommandDto;
import ru.caramel.juniperbot.web.dto.config.CustomCommandsContainerDto;

import java.util.List;

@RestController
public class CustomController extends BaseRestController {

    @Autowired
    private CustomCommandsDao commandsDao;

    @Autowired
    private CommandsHolderService holderService;

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
        container.setReservedKeys(holderService.getPublicCommandKeys());
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
