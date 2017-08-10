package ru.caramel.juniperbot.commands.audio;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import ru.caramel.juniperbot.audio.service.PlaybackManager;
import ru.caramel.juniperbot.commands.Command;
import ru.caramel.juniperbot.commands.DiscordCommand;
import ru.caramel.juniperbot.model.BotContext;
import ru.caramel.juniperbot.model.CommandSource;
import ru.caramel.juniperbot.model.exception.DiscordException;

@DiscordCommand(key = "скип", description = "Перейти к воспроизведению следующего трека", source = CommandSource.GUILD)
public class SkipCommand implements Command {

    @Autowired
    private PlaybackManager playbackManager;

    @Override
    public boolean doCommand(MessageReceivedEvent message, BotContext context) throws DiscordException {
        playbackManager.skipTrack(message.getGuild());
        return true;
    }
}
