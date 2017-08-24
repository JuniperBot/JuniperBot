package ru.caramel.juniperbot.commands.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
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
        group = CommandGroup.MUSIC)
public class PlayCommand extends AudioCommand {

    @Autowired
    private YouTubeClient youTubeClient;

    @Autowired
    protected ValidationService validationService;

    @Autowired
    protected AudioPlayerManager playerManager;

    @Override
    public boolean doInternal(MessageReceivedEvent message, BotContext context, String query) throws DiscordException {
        if (!message.getMessage().getAttachments().isEmpty()) {
            query = message.getMessage().getAttachments().get(0).getUrl();
        }
        if (StringUtils.isNumeric(query) && CollectionUtils.isNotEmpty(context.getSearchResults())) {
            int index = Integer.parseInt(query) - 1;
            if (index < 0 || index > context.getSearchResults().size() - 1) {
                messageManager.onQueueError(message.getChannel(), String.format("Введите номер от 1 до %s", context.getSearchResults().size()));
                return false;
            }
            query = context.getSearchResults().get(index);
        }
        if (!ResourceUtils.isUrl(query)) {
            String result = youTubeClient.searchForUrl(query);
            query = result != null ? result : query;
        }
        loadAndPlay(message.getTextChannel(), context, message.getAuthor(), query);
        context.setSearchResults(null);
        return true;
    }

    public void loadAndPlay(final TextChannel channel, final BotContext context, final User requestedBy, final String trackUrl) {
        PlaybackHandler musicManager = handlerService.getHandler(channel.getGuild());

        playerManager.loadItemOrdered(musicManager, trackUrl, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                try {
                    validationService.validateSingle(track, requestedBy, context);
                    musicManager.play(new TrackRequest(track, requestedBy, channel));
                } catch (ValidationException e) {
                    messageManager.onQueueError(channel, e.getMessage());
                }
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                try {
                    List<AudioTrack> tracks = validationService.filterPlaylist(playlist, requestedBy, context);
                    musicManager.play(tracks.stream().map(e -> new TrackRequest(e , requestedBy, channel)).collect(Collectors.toList()));
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
