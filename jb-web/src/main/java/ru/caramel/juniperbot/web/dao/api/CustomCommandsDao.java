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
package ru.caramel.juniperbot.web.dao.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.caramel.juniperbot.core.persistence.entity.GuildConfig;
import ru.caramel.juniperbot.module.custom.persistence.entity.CustomCommand;
import ru.caramel.juniperbot.module.custom.persistence.repository.CustomCommandRepository;
import ru.caramel.juniperbot.web.dao.AbstractDao;
import ru.caramel.juniperbot.web.dto.api.config.CustomCommandDto;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CustomCommandsDao extends AbstractDao {

    @Autowired
    private CustomCommandRepository commandRepository;

    @Transactional
    public List<CustomCommandDto> get(long guildId) {
        return apiMapper.getCustomCommandsDto(commandRepository.findAllByGuildId(guildId));
    }

    @Transactional
    public void save(List<CustomCommandDto> dtos, long guildId) {
        GuildConfig config = configService.getOrCreate(guildId);
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
                apiMapper.updateCustomCommand(e, command);
                result.add(command);
            }
        });

        // adding new commands
        Set<String> keys = customCommands.stream().map(CustomCommand::getKey).collect(Collectors.toSet());
        result.addAll(apiMapper.getCustomCommands(dtos.stream().filter(e -> e.getId() == null && !keys.contains(e.getKey())).collect(Collectors.toList())));
        result.forEach(e -> e.setConfig(config));

        commandRepository.save(preventDuplicates(result));

        // delete old
        commandRepository.delete(customCommands.stream().filter(e -> !result.contains(e)).collect(Collectors.toList()));
    }

    private List<CustomCommand> preventDuplicates(List<CustomCommand> commands) {
        Map<String, List<CustomCommand>> duplicated = commands.stream()
                .collect(Collectors.groupingBy(CustomCommand::getKey));
        duplicated.forEach((key, list) -> {
            if (list.size() > 1) {
                for (int i = 1; i < list.size(); i++) {
                    CustomCommand command = list.get(i);
                    command.setKey(command.getKey() + i);
                }
            }
        });
        return commands;
    }
}
