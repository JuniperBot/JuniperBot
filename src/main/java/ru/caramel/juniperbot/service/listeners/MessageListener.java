package ru.caramel.juniperbot.service.listeners;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.caramel.juniperbot.integration.discord.DiscordEventListener;
import ru.caramel.juniperbot.service.CommandsService;

@Component
public class MessageListener extends DiscordEventListener {

    @Autowired
    private CommandsService commandsService;

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        commandsService.onMessageReceived(event);
    }
}
