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

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.caramel.juniperbot.core.persistence.entity.GuildConfig;
import ru.caramel.juniperbot.core.service.CommandsHolderService;
import ru.caramel.juniperbot.web.dao.AbstractDao;
import ru.caramel.juniperbot.web.dto.api.config.CommandDto;
import ru.caramel.juniperbot.web.dto.api.config.CommandGroupDto;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CommandsDao extends AbstractDao {

    @Autowired
    private CommandsHolderService holderService;

    @Transactional
    public List<CommandGroupDto> get(long guildId) {
        GuildConfig config = configService.getOrCreate(guildId);
        Set<String> disabledSet = config.getDisabledCommands() != null
                ? new HashSet<>(Arrays.asList(config.getDisabledCommands())) : Collections.emptySet();
        return holderService.getDescriptors().entrySet().stream()
                .filter(e -> CollectionUtils.isNotEmpty(e.getValue()))
                .map(e -> {
                    CommandGroupDto groupDto = new CommandGroupDto();
                    groupDto.setKey(e.getKey());
                    groupDto.setCommands(e.getValue().stream().map(c -> {
                        CommandDto commandDto = new CommandDto();
                        commandDto.setKey(c.key());
                        commandDto.setEnabled(!disabledSet.contains(c.key()));
                        return commandDto;
                    }).collect(Collectors.toList()));
                    return groupDto;
                }).collect(Collectors.toList());
    }

    @Transactional
    public void save(CommandGroupDto dto, long guildId) {
        saveAll(Collections.singletonList(dto), guildId, true);
    }

    @Transactional
    public void save(CommandDto dto, long guildId) {
        saveAllCommands(Collections.singletonList(dto), guildId, true);
    }

    @Transactional
    public void saveAll(Collection<CommandGroupDto> dto, long guildId, boolean differential) {
        if (CollectionUtils.isEmpty(dto)) {
            return;
        }
        saveAllCommands(dto.stream().filter(e -> CollectionUtils.isNotEmpty(e.getCommands()))
                .flatMap(e -> e.getCommands().stream())
                .collect(Collectors.toSet()), guildId, differential);

    }

    @Transactional
    public void saveAllCommands(Collection<CommandDto> dto, long guildId, boolean differential) {
        if (CollectionUtils.isEmpty(dto)) {
            return;
        }
        GuildConfig config = configService.getOrCreate(guildId);
        Set<String> availableCommands = holderService.getPublicCommands().keySet();

        Set<String> currentDisabled = config.getDisabledCommands() != null ?
                new HashSet<>(Arrays.asList(config.getDisabledCommands()))
                : new HashSet<>();

        Set<String> toEnable = new HashSet<>();
        Set<String> toDisable = new HashSet<>();
        dto.stream()
                .filter(e -> e.getKey() != null && availableCommands.contains(e.getKey()))
                .forEach(e -> (e.isEnabled() ? toEnable : toDisable).add(e.getKey()));

        if (differential) {
            currentDisabled.removeAll(toEnable);
            currentDisabled.addAll(toDisable);
        } else {
            currentDisabled = toDisable;
        }

        config.setDisabledCommands(currentDisabled.toArray(new String[0]));
        configService.save(config);
    }
}
