package ru.caramel.juniperbot.commands;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import ru.caramel.juniperbot.configuration.DiscordConfig;
import ru.caramel.juniperbot.commands.model.BotContext;
import ru.caramel.juniperbot.integration.discord.model.DiscordException;

public abstract class AbstractCommand implements Command {

    @Autowired
    private DiscordConfig discordConfig;

    @Override
    public boolean doCommand(MessageReceivedEvent message, BotContext context) throws DiscordException {
        String content = message.getMessage().getContent();
        String prefix = discordConfig.getPrefix() + this.getClass().getAnnotation(DiscordCommand.class).key();
        return doCommand(message, context, content.substring(prefix.length(), content.length()).trim());
    }

    public abstract boolean doCommand(MessageReceivedEvent message, BotContext context, String content) throws DiscordException;
}
