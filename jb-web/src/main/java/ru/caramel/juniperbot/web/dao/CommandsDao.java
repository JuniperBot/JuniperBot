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

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.caramel.juniperbot.core.persistence.entity.CommandConfig;
import ru.caramel.juniperbot.core.persistence.entity.GuildConfig;
import ru.caramel.juniperbot.core.service.CommandConfigService;
import ru.caramel.juniperbot.core.service.CommandsHolderService;
import ru.caramel.juniperbot.web.dto.config.CommandDto;
import ru.caramel.juniperbot.web.dto.config.CommandGroupDto;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CommandsDao extends AbstractDao {

    @Autowired
    private CommandsHolderService holderService;

    @Autowired
    private CommandConfigService commandConfigService;

    @Transactional
    public List<CommandGroupDto> get(long guildId) {
        Map<String, CommandConfig> commandConfigs = commandConfigService.findAllMap(guildId);
        return holderService.getDescriptors().entrySet().stream()
                .filter(e -> CollectionUtils.isNotEmpty(e.getValue()))
                .map(e -> {
                    CommandGroupDto groupDto = new CommandGroupDto();
                    groupDto.setKey(e.getKey());
                    groupDto.setCommands(e.getValue().stream().map(c -> {
                        CommandConfig commandConfig = commandConfigs.get(c.key());
                        if (commandConfig != null) {
                            return apiMapper.getCommandDto(commandConfig);
                        }
                        CommandDto commandDto = new CommandDto();
                        commandDto.setKey(c.key());
                        commandDto.setEnabled(true);
                        return commandDto;
                    }).collect(Collectors.toList()));
                    return groupDto;
                }).collect(Collectors.toList());
    }

    @Transactional
    public void save(CommandGroupDto dto, long guildId) {
        saveAll(Collections.singletonList(dto), guildId);
    }

    @Transactional
    public void save(CommandDto dto, long guildId) {
        saveAllCommands(Collections.singletonList(dto), guildId);
    }

    @Transactional
    public void saveAll(Collection<CommandGroupDto> dto, long guildId) {
        if (CollectionUtils.isEmpty(dto)) {
            return;
        }
        saveAllCommands(dto.stream().filter(e -> CollectionUtils.isNotEmpty(e.getCommands()))
                .flatMap(e -> e.getCommands().stream())
                .collect(Collectors.toSet()), guildId);

    }

    @Transactional
    public void saveAllCommands(Collection<CommandDto> dto, long guildId) {
        if (CollectionUtils.isEmpty(dto)) {
            return;
        }
        Map<String, CommandConfig> commandConfigs = commandConfigService.findAllMap(guildId);
        Set<String> availableCommands = holderService.getPublicCommands().keySet();
        List<CommandConfig> toSave = dto.stream()
                .filter(e -> e.getKey() != null && availableCommands.contains(e.getKey()))
                .map(e -> {
                    CommandConfig commandConfig = commandConfigs.get(e.getKey());
                    if (commandConfig == null) {
                        commandConfig = new CommandConfig();
                        commandConfig.setGuildId(guildId);
                    }
                    apiMapper.updateCommandConfig(e, commandConfig);
                    return commandConfig;
                })
                .collect(Collectors.toList());
        commandConfigService.save(toSave);
    }
}
