package ru.caramel.juniperbot.commands.audio;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ResourceUtils;
import ru.caramel.juniperbot.audio.service.MessageManager;
import ru.caramel.juniperbot.audio.service.PlaybackManager;
import ru.caramel.juniperbot.commands.base.Command;
import ru.caramel.juniperbot.commands.model.CommandGroup;
import ru.caramel.juniperbot.commands.model.DiscordCommand;
import ru.caramel.juniperbot.integration.youtube.YouTubeClient;
import ru.caramel.juniperbot.commands.model.BotContext;
import ru.caramel.juniperbot.commands.model.CommandSource;
import ru.caramel.juniperbot.integration.discord.model.DiscordException;

@DiscordCommand(
        key = "плей",
        description = "Воспроизвести композицию в голосовом канале по названию, указанному URL или приложенному файлу",
        source = CommandSource.GUILD,
        group = CommandGroup.MUSIC)
public class PlayCommand implements Command {

    @Autowired
    private PlaybackManager playbackManager;

    @Autowired
    private YouTubeClient youTubeClient;

    @Autowired
    private MessageManager messageManager;

    @Override
    public boolean doCommand(MessageReceivedEvent message, BotContext context, String query) throws DiscordException {
        if (!playbackManager.validateChannel(message.getTextChannel(), message.getAuthor())) {
            return false;
        }
        if (!message.getMessage().getAttachments().isEmpty()) {
            query = message.getMessage().getAttachments().get(0).getUrl();
        }
        if (StringUtils.isNumeric(query) && CollectionUtils.isNotEmpty(context.getSearchResults())) {
            int index = Integer.parseInt(query) - 1;
            if (index < 0 || index > context.getSearchResults().size() - 1) {
                messageManager.onError(message.getChannel(), String.format("Введите номер от 1 до %s", context.getSearchResults().size()));
                return false;
            }
            query = context.getSearchResults().get(index);
        }
        if (!ResourceUtils.isUrl(query)) {
            String result = youTubeClient.searchForUrl(query);
            query = result != null ? result : query;
        }
        playbackManager.loadAndPlay(message.getTextChannel(), context, message.getAuthor(), query);
        context.setSearchResults(null);
        return true;
    }
}
