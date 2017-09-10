package ru.caramel.juniperbot.service.impl;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.caramel.juniperbot.commands.Command;
import ru.caramel.juniperbot.commands.model.CommandGroup;
import ru.caramel.juniperbot.commands.model.DiscordCommand;
import ru.caramel.juniperbot.service.CommandsHolderService;
import ru.caramel.juniperbot.service.MessageService;

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
