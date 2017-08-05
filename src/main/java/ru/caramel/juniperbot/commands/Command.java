package ru.caramel.juniperbot.commands;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import ru.caramel.juniperbot.model.BotContext;
import ru.caramel.juniperbot.model.exception.DiscordException;

public interface Command {

    boolean doCommand(MessageReceivedEvent message, BotContext context, String[] args) throws DiscordException;

}
