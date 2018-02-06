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
package ru.caramel.juniperbot.web.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.caramel.juniperbot.core.persistence.entity.GuildConfig;
import ru.caramel.juniperbot.module.custom.persistence.entity.CustomCommand;
import ru.caramel.juniperbot.module.custom.persistence.repository.CustomCommandRepository;
import ru.caramel.juniperbot.web.dto.CustomCommandDto;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CustomCommandsDao extends AbstractDao {

    @Autowired
    private CustomCommandRepository commandRepository;

    @Transactional
    public List<CustomCommandDto> getCommands(long serverId) {
        return mapper.getCommandsDto(commandRepository.findAllByGuildId(serverId));
    }

    @Transactional
    public void saveCommands(List<CustomCommandDto> commands, long serverId) {
        GuildConfig config = configService.getOrCreate(serverId);
        List<CustomCommand> customCommands = commandRepository.findAllByGuildId(serverId);
        if (commands == null) {
            commands = Collections.emptyList();
        }
        List<CustomCommand> result = new ArrayList<>();

        // update existing
        commands.stream().filter(e -> e.getId() != null).forEach(e -> {
            CustomCommand command = customCommands.stream().filter(e1 -> Objects.equals(e1.getId(), e.getId())).findFirst().orElse(null);
            if (command != null) {
                mapper.updateCommand(e, command);
                result.add(command);
            }
        });

        // adding new commands
        Set<String> keys = customCommands.stream().map(CustomCommand::getKey).collect(Collectors.toSet());
        result.addAll(mapper.getCommands(commands.stream().filter(e -> e.getId() == null && !keys.contains(e.getKey())).collect(Collectors.toList())));
        result.forEach(e -> e.setConfig(config));

        commandRepository.save(result);

        // delete old
        commandRepository.delete(customCommands.stream().filter(e -> !result.contains(e)).collect(Collectors.toList()));
    }
}
