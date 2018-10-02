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
import ru.caramel.juniperbot.core.persistence.entity.CommandConfig;
import ru.caramel.juniperbot.module.custom.persistence.entity.CustomCommand;
import ru.caramel.juniperbot.module.custom.persistence.repository.CustomCommandRepository;
import ru.caramel.juniperbot.web.dto.config.CustomCommandDto;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CustomCommandsDao extends AbstractDao {

    @Autowired
    private CustomCommandRepository commandRepository;

    @Transactional
    public List<CustomCommandDto> get(long guildId) {
        return commandRepository.findAllByGuildId(guildId).stream().map(e -> {
            CustomCommandDto dto = apiMapper.getCustomCommandDto(e);
            CommandConfig commandConfig = e.getCommandConfig();
            if (commandConfig == null) {
                commandConfig = new CommandConfig();
                commandConfig.setKey(e.getKey());
                commandConfig.setGuildId(guildId);
                e.setCommandConfig(commandConfig);
            }
            apiMapper.updateCommandDto(e.getCommandConfig(), dto);
            return dto;
        }).collect(Collectors.toList());
    }

    @Transactional
    public void save(List<CustomCommandDto> dtos, long guildId) {
        List<CustomCommand> customCommands = commandRepository.findAllByGuildId(guildId);
        if (dtos == null) {
            dtos = Collections.emptyList();
        }
        List<CustomCommand> result = new ArrayList<>();

        // update existing
        dtos.stream().filter(e -> e.getId() != null).forEach(e -> {
            if (e.getId() < 0) {
                e.setId(null);
            }
            CustomCommand command = customCommands.stream().filter(e1 -> Objects.equals(e1.getId(), e.getId())).findFirst().orElse(null);
            if (command != null) {
                CommandConfig commandConfig = command.getCommandConfig();
                if (commandConfig == null) {
                    commandConfig = new CommandConfig();
                    commandConfig.setGuildId(guildId);
                    command.setCommandConfig(commandConfig);
                }
                commandConfig.setKey(e.getKey());
                apiMapper.updateCommandConfig(e, command.getCommandConfig());
                apiMapper.updateCustomCommand(e, command);
                result.add(command);
            }
        });

        // adding new commands
        Set<String> keys = customCommands.stream().map(CustomCommand::getKey).collect(Collectors.toSet());
        result.addAll(dtos.stream().filter(e -> e.getId() == null && !keys.contains(e.getKey())).map(e -> {
            CustomCommand customCommand = new CustomCommand();
            customCommand.setGuildId(guildId);
            CommandConfig commandConfig = new CommandConfig();
            commandConfig.setGuildId(guildId);
            commandConfig.setKey(e.getKey());
            customCommand.setCommandConfig(commandConfig);
            apiMapper.updateCommandConfig(e, commandConfig);
            apiMapper.updateCustomCommand(e, customCommand);
            return customCommand;
        }).collect(Collectors.toList()));

        commandRepository.saveAll(preventDuplicates(result));

        // delete old
        commandRepository.deleteAll(customCommands.stream().filter(e -> !result.contains(e)).collect(Collectors.toList()));
    }

    private List<CustomCommand> preventDuplicates(List<CustomCommand> commands) {
        Map<String, List<CustomCommand>> duplicated = commands.stream()
                .collect(Collectors.groupingBy(CustomCommand::getKey));
        duplicated.forEach((key, list) -> {
            if (list.size() > 1) {
                for (int i = 1; i < list.size(); i++) {
                    CustomCommand command = list.get(i);
                    command.setKey(command.getKey() + i);
                    command.getCommandConfig().setKey(command.getKey() + i);
                }
            }
        });
        return commands;
    }
}
