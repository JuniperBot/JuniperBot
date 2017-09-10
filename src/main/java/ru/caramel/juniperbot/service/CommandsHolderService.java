package ru.caramel.juniperbot.service;

import ru.caramel.juniperbot.commands.Command;
import ru.caramel.juniperbot.commands.model.CommandGroup;
import ru.caramel.juniperbot.commands.model.DiscordCommand;

import java.util.List;
import java.util.Map;

public interface CommandsHolderService {

    Map<String, Command> getCommands();

    Map<CommandGroup, List<DiscordCommand>> getDescriptors();

    Command getByLocale(String localizedKey);

    Command getByLocale(String localizedKey, boolean anyLocale);

}
