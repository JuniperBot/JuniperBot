package ru.caramel.juniperbot.commands.base;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import ru.caramel.juniperbot.commands.model.DiscordCommand;
import ru.caramel.juniperbot.commands.model.BotContext;
import ru.caramel.juniperbot.integration.discord.model.DiscordException;

public abstract class AbstractCommand implements Command {

    @Override
    public boolean doCommand(MessageReceivedEvent message, BotContext context) throws DiscordException {
        String content = message.getMessage().getContent();
        String prefix = context.getPrefix() + this.getClass().getAnnotation(DiscordCommand.class).key();
        return doCommand(message, context, content.substring(prefix.length(), content.length()).trim());
    }

    public abstract boolean doCommand(MessageReceivedEvent message, BotContext context, String content) throws DiscordException;
}
