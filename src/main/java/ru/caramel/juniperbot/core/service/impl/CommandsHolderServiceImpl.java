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
package ru.caramel.juniperbot.core.service.impl;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.caramel.juniperbot.core.model.Command;
import ru.caramel.juniperbot.core.model.enums.CommandGroup;
import ru.caramel.juniperbot.core.model.DiscordCommand;
import ru.caramel.juniperbot.core.service.CommandsHolderService;
import ru.caramel.juniperbot.core.service.MessageService;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CommandsHolderServiceImpl implements CommandsHolderService {

    @Autowired
    private MessageService messageService;

    private Map<String, Command> localizedCommands;

    @Getter
    private Map<String, Command> commands;

    private Map<CommandGroup, List<DiscordCommand>> descriptors;

    @Override
    public Command getByLocale(String localizedKey) {
        return localizedCommands.get(localizedKey);
    }

    @Override
    public Command getByLocale(String localizedKey, boolean anyLocale) {
        return getByLocale(localizedKey); // TODO maybe should be implemented later
    }

    @Autowired
    private void registerCommands(List<Command> commands) {
        this.localizedCommands = new HashMap<>();
        this.commands = new HashMap<>();
        commands.stream().filter(e -> e.getClass().isAnnotationPresent(DiscordCommand.class)).forEach(e -> {
            String localized = messageService.getMessage(e.getClass().getAnnotation(DiscordCommand.class).key());
            this.localizedCommands.put(localized, e);
            this.commands.put(e.getClass().getAnnotation(DiscordCommand.class).key(), e);
        });
    }

    @Override
    public Map<CommandGroup, List<DiscordCommand>> getDescriptors() {
        if (descriptors == null) {
            List<DiscordCommand> discordCommands = commands.entrySet().stream()
                    .map(e -> e.getValue().getClass().getAnnotation(DiscordCommand.class))
                    .filter(e -> !e.hidden())
                    .collect(Collectors.toList());
            discordCommands.sort(Comparator.comparingInt(DiscordCommand::priority));
            descriptors = discordCommands
                    .stream().collect(Collectors.groupingBy(DiscordCommand::group, LinkedHashMap::new, Collectors.toList()));
        }
        return descriptors;
    }
}
