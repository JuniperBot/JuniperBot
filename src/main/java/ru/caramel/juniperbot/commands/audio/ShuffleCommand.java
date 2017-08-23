package ru.caramel.juniperbot.commands.audio;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import ru.caramel.juniperbot.audio.service.MessageManager;
import ru.caramel.juniperbot.audio.service.PlaybackManager;
import ru.caramel.juniperbot.commands.base.Command;
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
public class ShuffleCommand implements Command {

    @Autowired
    private PlaybackManager playbackManager;

    @Autowired
    private MessageManager messageManager;

    @Override
    public boolean doCommand(MessageReceivedEvent message, BotContext context, String content) throws DiscordException {
        if (!playbackManager.validateChannel(message.getTextChannel(), message.getAuthor())) {
            return false;
        }
        if (playbackManager.shuffleTracks(message.getGuild())) {
            messageManager.onMessage(message.getChannel(), "Очередь воспроизведения перемешана :cyclone: ");
        } else {
            messageManager.onEmptyQueue(message.getChannel());
        }
        return true;
    }
}
