package ru.caramel.juniperbot.service;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import ru.caramel.juniperbot.commands.Command;
import ru.caramel.juniperbot.commands.model.CommandGroup;
import ru.caramel.juniperbot.commands.model.DiscordCommand;
import ru.caramel.juniperbot.model.CustomCommandDto;

import java.util.List;
import java.util.Map;

public interface CommandsService {

    void onMessageReceived(MessageReceivedEvent event);

    Map<String, Command> getCommands();

    Map<CommandGroup, List<DiscordCommand>> getDescriptors();

    Command getByLocale(String localizedKey);

    Command getByLocale(String localizedKey, boolean anyLocale);

    List<CustomCommandDto> getCustomForView(long serverId);

    void saveCommands(List<CustomCommandDto> commands, long serverId);
}
