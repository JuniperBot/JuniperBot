package ru.caramel.juniperbot.commands;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import ru.caramel.juniperbot.model.BotContext;
import ru.caramel.juniperbot.model.exception.DiscordException;
import ru.caramel.juniperbot.utils.ParseUtils;

public abstract class ParameterizedCommand extends AbstractCommand {
    @Override
    public boolean doCommand(MessageReceivedEvent message, BotContext context, String content) throws DiscordException {
        return doCommand(message, context, ParseUtils.readArgs(content));
    }

    protected abstract boolean doCommand(MessageReceivedEvent message, BotContext context, String[] args) throws DiscordException;
}
