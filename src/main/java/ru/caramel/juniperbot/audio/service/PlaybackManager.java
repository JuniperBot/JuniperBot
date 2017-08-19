package ru.caramel.juniperbot.audio.service;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import ru.caramel.juniperbot.audio.model.TrackRequest;
import ru.caramel.juniperbot.commands.model.BotContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class PlaybackManager {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private AudioPlayerManager playerManager;

    @Autowired
    private MessageManager messageManager;

    private Map<Long, GuildPlaybackManager> musicManagers = new HashMap<>();

    private synchronized GuildPlaybackManager getGuildAudioPlayer(Guild guild) {
        long guildId = Long.parseLong(guild.getId());
        return musicManagers.computeIfAbsent(guildId,
                e -> applicationContext.getBean(GuildPlaybackManager.class));
    }

    public void loadAndPlay(final TextChannel channel, final BotContext context, final User requestedBy, final String trackUrl) {
        GuildPlaybackManager musicManager = getGuildAudioPlayer(channel.getGuild());
        Long queueLimit = context.getConfig().getMusicQueueLimit();
        Long durationLimit = context.getConfig().getMusicDurationLimit();
        Long duplicateLimit = context.getConfig().getMusicDuplicateLimit();

        playerManager.loadItemOrdered(musicManager, trackUrl, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                if (queueLimit != null) {
                    List<TrackRequest> userQueue = musicManager.getQueue(requestedBy);
                    if (userQueue.size() >= queueLimit) {
                        messageManager.onError(channel, String.format("Вы превысили лимит треков в очереди (%s) :raised_hand:", queueLimit));
                        return;
                    }
                }
                if (duplicateLimit != null) {
                    List<TrackRequest> userQueue = musicManager.getQueue().stream().filter(e -> compareTracks(e.getTrack(), track)).collect(Collectors.toList());
                    if (userQueue.size() >= duplicateLimit) {
                        messageManager.onError(channel, String.format("Превышен лимит одинаковых треков в очереди (%s) :raised_hand:", duplicateLimit));
                        return;
                    }
                }
                if (durationLimit != null && track.getDuration() >= (durationLimit * 60000)) {
                    messageManager.onError(channel, String.format("Запрещено ставить треки длительностью более %d мин :raised_hand:", durationLimit));
                    return;
                }
                musicManager.play(new TrackRequest(track, requestedBy, channel));
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                if (!Boolean.TRUE.equals(context.getConfig().getMusicPlaylistEnabled())) {
                    messageManager.onError(channel, "Добавление плейлистов отключено на этом сервере :raised_hand:");
                    return;
                }

                List<AudioTrack> tracks = playlist.getTracks();
                if (!tracks.isEmpty() && durationLimit != null) {
                    tracks = tracks.stream().filter(e -> e.getDuration() < (durationLimit * 60000)).collect(Collectors.toList());
                    if (tracks.isEmpty()) {
                        messageManager.onError(channel, String.format("Ни один трек плейлиста не подходит под ограничение %d мин :raised_hand:", durationLimit));
                        return;
                    }
                }

                if (!tracks.isEmpty() && duplicateLimit != null) {
                    List<TrackRequest> queue = musicManager.getQueue();
                    tracks = tracks.stream().filter(e -> queue.stream().filter(e2 -> compareTracks(e2.getTrack(), e)).count() < duplicateLimit).collect(Collectors.toList());
                    if (tracks.isEmpty()) {
                        messageManager.onError(channel, String.format("Ни один трек плейлиста не подходит под ограничение %d одинаковых треков :raised_hand:", duplicateLimit));
                        return;
                    }
                }

                if (tracks.isEmpty()) {
                    messageManager.onError(channel, "Указанный плейлист пуст");
                    return;
                }

                if (queueLimit != null) {
                    List<TrackRequest> userQueue = musicManager.getQueue(requestedBy);
                    int availableSlots = queueLimit.intValue() - userQueue.size();
                    if (availableSlots <= 0) {
                        messageManager.onError(channel, String.format("Вы превысили лимит треков в очереди (%s) :raised_hand:", queueLimit));
                        return;
                    }
                    if (tracks.size() > availableSlots) {
                        tracks = tracks.subList(0, availableSlots);
                    }
                }
                musicManager.play(tracks.stream().map(e -> new TrackRequest(e , requestedBy, channel)).collect(Collectors.toList()));
            }

            @Override
            public void noMatches() {
                messageManager.onNoMatches(channel, trackUrl);
            }

            @Override
            public void loadFailed(FriendlyException e) {
                messageManager.onError(channel, e);
            }
        });
    }

    public void skipTrack(Guild guild) {
        getGuildAudioPlayer(guild).nextTrack();
    }

    public boolean isInChannel(Guild guild, User user) {
        return getGuildAudioPlayer(guild).isInChannel(user);
    }

    public boolean validateChannel(TextChannel channel, User user) {
        boolean result = isInChannel(channel.getGuild(), user);
        if (!result) {
            messageManager.onDisallowed(channel);
        }
        return result;
    }

    public boolean pauseTrack(Guild guild) {
        return getGuildAudioPlayer(guild).pauseTrack();
    }

    public boolean shuffleTracks(Guild guild) {
        return getGuildAudioPlayer(guild).shuffle();
    }

    public boolean resumeTrack(Guild guild) {
        return getGuildAudioPlayer(guild).resumeTrack();
    }

    public boolean stop(Guild guild) {
        return getGuildAudioPlayer(guild).stop();
    }

    public List<TrackRequest> getQueue(Guild guild) {
        return getGuildAudioPlayer(guild).getQueue();
    }

    private boolean compareTracks(AudioTrack track1, AudioTrack track2) {
        if (Objects.equals(track1, track2)) {
            return true;
        }
        AudioTrackInfo info1 = track1.getInfo();
        AudioTrackInfo info2 = track2.getInfo();
        if (info1 != null && info2 != null) {
            return Objects.equals(info1.uri, info2.uri)
                    && Objects.equals(info1.uri, info2.uri)
                    && Objects.equals(info1.title, info2.title)
                    && Objects.equals(info1.author, info2.author)
                    && Objects.equals(info1.identifier, info2.identifier)
                    && Objects.equals(info1.length, info2.length)
                    && Objects.equals(info1.isStream, info2.isStream);
        }
        return Objects.equals(info1, info1);
    }
}
