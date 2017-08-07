package ru.caramel.juniperbot.commands.audio;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import ru.caramel.juniperbot.audio.PlaybackManager;
import ru.caramel.juniperbot.commands.Command;
import ru.caramel.juniperbot.commands.DiscordCommand;
import ru.caramel.juniperbot.model.BotContext;
import ru.caramel.juniperbot.model.exception.DiscordException;
import ru.caramel.juniperbot.utils.ParseUtils;

@DiscordCommand(key = "скип", description = "Перейти к воспроизведению следующего трека")
public class SkipCommand implements Command {

    @Autowired
    private PlaybackManager playbackManager;

    @Override
    public boolean doCommand(MessageReceivedEvent message, BotContext context) throws DiscordException {
        String[] args = ParseUtils.readArgs(message.getMessage().getContent(), true);
        if (args.length == 0) {
            return false;
        }
        playbackManager.skipTrack(message.getTextChannel());
        return true;
    }
}
