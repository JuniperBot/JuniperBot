package ru.caramel.juniperbot.commands.audio;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import ru.caramel.juniperbot.audio.service.PlaybackManager;
import ru.caramel.juniperbot.commands.DiscordCommand;
import ru.caramel.juniperbot.commands.ParameterizedCommand;
import ru.caramel.juniperbot.model.BotContext;
import ru.caramel.juniperbot.model.exception.DiscordException;

@DiscordCommand(key = "плей", description = "Воспроизвести музыку в голосовом канале по указанному URL")
public class PlayCommand extends ParameterizedCommand {

    @Autowired
    private PlaybackManager playbackManager;

    @Override
    public boolean doCommand(MessageReceivedEvent message, BotContext context, String[] args) throws DiscordException {
        if (args.length == 0) {
            return false;
        }
        playbackManager.loadAndPlay(message.getTextChannel(), message.getAuthor(), args[0]);
        return true;
    }
}
