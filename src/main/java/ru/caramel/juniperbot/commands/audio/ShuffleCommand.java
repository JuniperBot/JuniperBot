package ru.caramel.juniperbot.commands.audio;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import ru.caramel.juniperbot.commands.model.BotContext;
import ru.caramel.juniperbot.commands.model.CommandGroup;
import ru.caramel.juniperbot.commands.model.CommandSource;
import ru.caramel.juniperbot.commands.model.DiscordCommand;
import ru.caramel.juniperbot.integration.discord.model.DiscordException;

@DiscordCommand(
        key = "перемешать",
        description = "Перемешать очередь воспроизведения",
        source = CommandSource.GUILD,
        group = CommandGroup.MUSIC)
public class ShuffleCommand extends AudioCommand {

    @Override
    public boolean doInternal(MessageReceivedEvent message, BotContext context, String content) throws DiscordException {
        if (handlerService.shuffleTracks(message.getGuild())) {
            messageManager.onMessage(message.getChannel(), "Очередь воспроизведения перемешана :cyclone: ");
        } else {
            messageManager.onEmptyQueue(message.getChannel());
        }
        return true;
    }
}
