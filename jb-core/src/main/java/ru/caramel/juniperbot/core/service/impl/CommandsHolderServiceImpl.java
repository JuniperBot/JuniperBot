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
import ru.caramel.juniperbot.core.model.DiscordCommand;
import ru.caramel.juniperbot.core.service.CommandsHolderService;
import ru.caramel.juniperbot.core.service.ContextService;
import ru.caramel.juniperbot.core.service.MessageService;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CommandsHolderServiceImpl implements CommandsHolderService {

    @Autowired
    private MessageService messageService;

    @Autowired
    private ContextService contextService;

    private Map<Locale, Map<String, Command>> localizedCommands;

    @Getter
    private Map<String, Command> commands;

    @Getter
    private Map<String, Command> publicCommands;

    private Map<String, List<DiscordCommand>> descriptors;

    @Override
    public Command getByLocale(String localizedKey) {
        Map<String, Command> commandMap = getLocalizedMap();
        return commandMap != null ? commandMap.get(localizedKey) : null;
    }

    private Map<String, Command> getLocalizedMap() {
        return localizedCommands.get(contextService.getLocale());
    }

    @Override
    public Command getByLocale(String localizedKey, boolean anyLocale) {
        if (anyLocale) {
            for (Map<String, Command> commandMap : localizedCommands.values()) {
                if (commandMap.containsKey(localizedKey)) {
                    return commandMap.get(localizedKey);
                }
            }
        }
        return getByLocale(localizedKey);
    }

    @Autowired
    private void registerCommands(List<Command> commands) {
        this.localizedCommands = new HashMap<>();
        this.commands = new HashMap<>();
        this.publicCommands = new HashMap<>();
        Collection<Locale> locales = contextService.getSupportedLocales().values();
        commands.stream().filter(e -> e.getClass().isAnnotationPresent(DiscordCommand.class)).forEach(e -> {
            DiscordCommand annotation = e.getClass().getAnnotation(DiscordCommand.class);
            String rawKey = annotation.key();
            this.commands.put(rawKey, e);
            if (!annotation.hidden()) {
                this.publicCommands.put(rawKey, e);
            }
            for (Locale locale : locales) {
                Map<String, Command> localeCommands = localizedCommands.computeIfAbsent(locale, e2 -> new HashMap<>());
                String localizedKey = messageService.getMessage(rawKey, locale);
                localeCommands.put(localizedKey, e);
            }
        });
    }

    @Override
    public Map<String, List<DiscordCommand>> getDescriptors() {
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
