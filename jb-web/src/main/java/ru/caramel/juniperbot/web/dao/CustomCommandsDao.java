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
import ru.caramel.juniperbot.core.command.persistence.CommandConfig;
import ru.caramel.juniperbot.core.command.persistence.CustomCommand;
import ru.caramel.juniperbot.core.command.persistence.CustomCommandRepository;
import ru.caramel.juniperbot.web.dto.config.CustomCommandDto;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CustomCommandsDao extends AbstractDao {

    @Autowired
    private CustomCommandRepository commandRepository;

    @Autowired
    private MessageTemplateDao templateDao;

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
            CustomCommand command = customCommands.stream().filter(e1 -> Objects.equals(e1.getId(), e.getId())).findFirst().orElse(null);
            if (command != null) {
                updateCommandConfig(command, e);
                apiMapper.updateCustomCommand(e, command);
                command.setMessageTemplate(templateDao.updateOrCreate(e.getMessageTemplate(), command.getMessageTemplate()));
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
            customCommand.setMessageTemplate(templateDao.updateOrCreate(e.getMessageTemplate(), null));
            return customCommand;
        }).collect(Collectors.toList()));

        commandRepository.saveAll(preventDuplicates(result));

        // delete old
        commandRepository.deleteAll(customCommands.stream().filter(e -> !result.contains(e)).collect(Collectors.toList()));
    }

    @Transactional
    public boolean saveConfig(CustomCommandDto dto, long guildId) {
        if (dto.getId() == null) {
            return false;
        }
        CustomCommand command = commandRepository.findById(dto.getId()).orElse(null);
        if (command == null || command.getGuildId() != guildId) {
            return false;
        }
        updateCommandConfig(command, dto);
        commandRepository.save(command);
        return true;
    }

    private void updateCommandConfig(CustomCommand command, CustomCommandDto dto) {
        CommandConfig commandConfig = command.getCommandConfig();
        if (commandConfig == null) {
            commandConfig = new CommandConfig();
            commandConfig.setGuildId(command.getGuildId());
            command.setCommandConfig(commandConfig);
        }
        commandConfig.setKey(dto.getKey() != null ? dto.getKey() : "#cmd_" + dto.getId());
        apiMapper.updateCommandConfig(dto, commandConfig);
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
