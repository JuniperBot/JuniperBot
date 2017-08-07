package ru.caramel.juniperbot.commands;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import ru.caramel.juniperbot.model.BotContext;
import ru.caramel.juniperbot.model.exception.DiscordException;
import ru.caramel.juniperbot.utils.ParseUtils;

public abstract class ParameterizedCommand implements Command {
    @Override
    public boolean doCommand(MessageReceivedEvent message, BotContext context) throws DiscordException {
        String[] args = ParseUtils.readArgs(message.getMessage().getContent(), true);
        return doCommand(message, context, args);
    }

    protected abstract boolean doCommand(MessageReceivedEvent message, BotContext context, String[] args) throws DiscordException;
}
