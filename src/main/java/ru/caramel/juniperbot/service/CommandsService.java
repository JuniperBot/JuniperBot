package ru.caramel.juniperbot.service;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import ru.caramel.juniperbot.commands.Command;
import ru.caramel.juniperbot.model.CustomCommandDto;

import java.util.List;
import java.util.Map;

public interface CommandsService {

    void onMessageReceived(MessageReceivedEvent event);

    Map<String, Command> getCommands();

    List<CustomCommandDto> getCustomForView(long serverId);

    void saveCommands(List<CustomCommandDto> commands, long serverId);
}
