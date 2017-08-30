package ru.caramel.juniperbot.commands.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ResourceUtils;
import ru.caramel.juniperbot.audio.service.PlaybackInstance;
import ru.caramel.juniperbot.audio.model.TrackRequest;
import ru.caramel.juniperbot.audio.service.*;
import ru.caramel.juniperbot.commands.model.*;
import ru.caramel.juniperbot.integration.youtube.YouTubeClient;
import ru.caramel.juniperbot.integration.discord.model.DiscordException;

import java.util.List;
import java.util.stream.Collectors;

@DiscordCommand(
        key = "плей",
        description = "Воспроизвести композицию в голосовом канале по названию, указанному URL или приложенному файлу",
        source = CommandSource.GUILD,
        group = CommandGroup.MUSIC,
        priority = 104)
public class PlayCommand extends AudioCommand {

    @Autowired
    private YouTubeClient youTubeClient;

    @Autowired
    protected ValidationService validationService;

    @Override
    public boolean doInternal(MessageReceivedEvent message, BotContext context, String query) throws DiscordException {
        if (!message.getMessage().getAttachments().isEmpty()) {
            query = message.getMessage().getAttachments().get(0).getUrl();
        }
        if (StringUtils.isNumeric(query) && CollectionUtils.isNotEmpty(context.getSearchResults())) {
            int index = Integer.parseInt(query) - 1;
            query = getChoiceUrl(context, index);
            if (query == null) {
                messageManager.onQueueError(message.getChannel(), String.format("Введите номер от 1 до %s", context.getSearchResults().size()));
                return false;
            }
        }
        if (!ResourceUtils.isUrl(query)) {
            String result = youTubeClient.searchForUrl(query);
            query = result != null ? result : query;
        }
        loadAndPlay(message.getTextChannel(), context, message.getAuthor(), query);
        return true;
    }

    protected String getChoiceUrl(BotContext context, int index) {
        if (index < 0 || index > context.getSearchResults().size() - 1) {
            return null;
        }
        String query = context.getSearchResults().get(index);
        context.getSearchActions().forEach(e1 -> e1.cancel(true));
        context.setSearchActions(null);
        context.getSearchMessage().delete().queue();
        context.setSearchMessage(null);
        context.setSearchResults(null);
        return query;
    }

    protected void loadAndPlay(final TextChannel channel, final BotContext context, final User requestedBy, final String trackUrl) {
        PlaybackInstance instance = playerService.getInstance(channel.getGuild());
        playerService.getPlayerManager().loadItemOrdered(instance, trackUrl, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                try {
                    validationService.validateSingle(track, requestedBy, context);
                    playerService.play(new TrackRequest(track, requestedBy, channel));
                } catch (ValidationException e) {
                    messageManager.onQueueError(channel, e.getMessage());
                }
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                try {
                    List<AudioTrack> tracks = validationService.filterPlaylist(playlist, requestedBy, context);
                    playerService.play(tracks.stream().map(e -> new TrackRequest(e , requestedBy, channel)).collect(Collectors.toList()));
                } catch (ValidationException e) {
                    messageManager.onQueueError(channel, e.getMessage());
                }
            }

            @Override
            public void noMatches() {
                messageManager.onNoMatches(channel, trackUrl);
            }

            @Override
            public void loadFailed(FriendlyException e) {
                messageManager.onQueueError(channel, "Произошла ошибка :interrobang::" + e.getMessage());
            }
        });
    }
}
