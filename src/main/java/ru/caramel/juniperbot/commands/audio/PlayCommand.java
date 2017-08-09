package ru.caramel.juniperbot.commands.audio;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ResourceUtils;
import ru.caramel.juniperbot.audio.service.PlaybackManager;
import ru.caramel.juniperbot.commands.AbstractCommand;
import ru.caramel.juniperbot.commands.DiscordCommand;
import ru.caramel.juniperbot.integration.youtube.YouTubeClient;
import ru.caramel.juniperbot.model.BotContext;
import ru.caramel.juniperbot.model.CommandSource;
import ru.caramel.juniperbot.model.exception.DiscordException;

@DiscordCommand(key = "плей", description = "Воспроизвести композицию в голосовом канале по названию или указанному URL", source = CommandSource.GUILD)
public class PlayCommand extends AbstractCommand {

    @Autowired
    private PlaybackManager playbackManager;

    @Autowired
    private YouTubeClient youTubeClient;

    @Override
    public boolean doCommand(MessageReceivedEvent message, BotContext context, String query) throws DiscordException {
        if (!message.getMessage().getAttachments().isEmpty()) {
            query = message.getMessage().getAttachments().get(0).getUrl();
        }
        if (!ResourceUtils.isUrl(query)) {
            String result = youTubeClient.searchForUrl(query);
            query = result != null ? result : query;
        }
        playbackManager.loadAndPlay(message.getTextChannel(), message.getAuthor(), query);
        return true;
    }
}
