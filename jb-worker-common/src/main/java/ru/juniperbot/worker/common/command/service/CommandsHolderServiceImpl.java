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
package ru.juniperbot.worker.common.command.service;

import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.juniperbot.worker.common.command.model.Command;
import ru.juniperbot.worker.common.command.model.DiscordCommand;
import ru.juniperbot.worker.common.event.service.ContextService;
import ru.juniperbot.worker.common.message.service.MessageService;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CommandsHolderServiceImpl implements CommandsHolderService {

    @Autowired
    private MessageService messageService;

    @Autowired
    private ContextService contextService;

    @Getter
    private Set<String> publicCommandKeys;

    private Set<String> reverseCommandKeys;

    private Map<Locale, Map<String, Command>> localizedCommands;

    @Getter
    private Map<String, Command> commands;

    @Getter
    private Map<String, Command> publicCommands;

    private Map<String, List<DiscordCommand>> descriptors;

    @Override
    public Command getByLocale(String localizedKey) {
        return getByLocale(localizedKey, contextService.getLocale());
    }

    @Override
    public Command getByLocale(String localizedKey, String locale) {
        return getByLocale(localizedKey, contextService.getLocale(locale));
    }

    @Override
    public Command getByLocale(String localizedKey, Locale locale) {
        if (localizedKey != null) {
            localizedKey = localizedKey.toLowerCase();
        }
        Map<String, Command> commandMap = getLocalizedMap(locale);
        return commandMap != null ? commandMap.get(localizedKey) : null;
    }

    private Map<String, Command> getLocalizedMap() {
        return MapUtils.isNotEmpty(localizedCommands)
                ? localizedCommands.get(contextService.getLocale())
                : Collections.emptyMap();
    }

    private Map<String, Command> getLocalizedMap(Locale locale) {
        return MapUtils.isNotEmpty(localizedCommands)
                ? localizedCommands.get(locale)
                : Collections.emptyMap();
    }

    @Override
    public Command getByLocale(String localizedKey, boolean anyLocale) {
        if (MapUtils.isNotEmpty(localizedCommands) && anyLocale) {
            if (localizedKey != null) {
                localizedKey = localizedKey.toLowerCase();
            }
            for (Map<String, Command> commandMap : localizedCommands.values()) {
                if (commandMap.containsKey(localizedKey)) {
                    return commandMap.get(localizedKey);
                }
            }
        }
        return getByLocale(localizedKey);
    }

    @Override
    public boolean isAnyCommand(String key) {
        String reverseKey = StringUtils.reverse(key).toLowerCase();
        return CollectionUtils.isNotEmpty(reverseCommandKeys)
                && reverseCommandKeys.stream().anyMatch(reverseKey::startsWith);
    }

    @Autowired
    private void registerCommands(List<Command> commands) {
        this.localizedCommands = new HashMap<>();
        this.commands = new HashMap<>();
        this.publicCommands = new HashMap<>();
        this.publicCommandKeys = new HashSet<>();
        this.reverseCommandKeys = new HashSet<>();
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
                String localizedKey = messageService.getMessageByLocale(rawKey, locale);
                reverseCommandKeys.add(StringUtils.reverse(localizedKey));
                if (!annotation.hidden()) {
                    publicCommandKeys.add(localizedKey);
                }
                localeCommands.put(localizedKey, e);
            }
        });

        // lock everything
        this.localizedCommands = Collections.unmodifiableMap(this.localizedCommands);
        this.commands = Collections.unmodifiableMap(this.commands);
        this.publicCommands = Collections.unmodifiableMap(this.publicCommands);
        this.publicCommandKeys = Collections.unmodifiableSet(this.publicCommandKeys);
        this.reverseCommandKeys = Collections.unmodifiableSet(this.reverseCommandKeys);
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
                    .stream().collect(Collectors.groupingBy(e -> e.group()[0], LinkedHashMap::new, Collectors.toList()));
        }
        return descriptors;
    }
}
