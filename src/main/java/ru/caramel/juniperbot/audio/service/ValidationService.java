package ru.caramel.juniperbot.audio.service;

import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import net.dv8tion.jda.core.entities.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.caramel.juniperbot.audio.model.TrackRequest;
import ru.caramel.juniperbot.commands.model.BotContext;
import ru.caramel.juniperbot.commands.model.ValidationException;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ValidationService {

    @Autowired
    private PlayerService playerService;

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

    public void validateSingle(AudioTrack track, User requestedBy, BotContext context) throws ValidationException {
        Long queueLimit = context.getConfig().getMusicQueueLimit();
        Long durationLimit = context.getConfig().getMusicDurationLimit();
        Long duplicateLimit = context.getConfig().getMusicDuplicateLimit();
        PlaybackInstance instance = playerService.getInstance(context.getGuild());

        if (track.getInfo().isStream && !context.getConfig().isMusicStreamsEnabled()) {
            throw new ValidationException("Потоковое аудио запрещено на этом сервере :raised_hand:");
        }

        if (queueLimit != null) {
            List<TrackRequest> userQueue = instance.getQueue(requestedBy);
            if (userQueue.size() >= queueLimit) {
                throw new ValidationException(String.format("Вы превысили лимит треков в очереди (%s) :raised_hand:", queueLimit));
            }
        }
        if (duplicateLimit != null) {
            List<TrackRequest> userQueue = instance.getQueue().stream().filter(e -> compareTracks(e.getTrack(), track)).collect(Collectors.toList());
            if (userQueue.size() >= duplicateLimit) {
                throw new ValidationException(String.format("Превышен лимит одинаковых треков в очереди (%s) :raised_hand:", duplicateLimit));
            }
        }
        if (!track.getInfo().isStream && durationLimit != null && track.getDuration() >= (durationLimit * 60000)) {
            throw new ValidationException(String.format("Запрещено ставить треки длительностью более %d мин :raised_hand:", durationLimit));
        }
    }

    public List<AudioTrack> filterPlaylist(AudioPlaylist playlist, User requestedBy, BotContext context) throws ValidationException {
        Long queueLimit = context.getConfig().getMusicQueueLimit();
        Long durationLimit = context.getConfig().getMusicDurationLimit();
        Long duplicateLimit = context.getConfig().getMusicDuplicateLimit();
        PlaybackInstance instance = playerService.getInstance(context.getGuild());

        if (!Boolean.TRUE.equals(context.getConfig().getMusicPlaylistEnabled())) {
            throw new ValidationException("Плейлисты запрещены на этом сервере :raised_hand:");
        }

        List<AudioTrack> tracks = playlist.getTracks();
        if (!tracks.isEmpty() && !context.getConfig().isMusicStreamsEnabled()) {
            tracks = tracks.stream().filter(e -> !e.getInfo().isStream).collect(Collectors.toList());
            if (tracks.isEmpty()) {
                throw new ValidationException("Потоковое аудио запрещено на этом сервере :raised_hand:");
            }
        }

        if (!tracks.isEmpty() && durationLimit != null) {
            tracks = tracks.stream().filter(e -> e.getInfo().isStream || e.getDuration() < (durationLimit * 60000)).collect(Collectors.toList());
            if (tracks.isEmpty()) {
                throw new ValidationException(String.format("Ни один трек плейлиста не подходит под ограничение %d мин :raised_hand:", durationLimit));
            }
        }

        if (!tracks.isEmpty() && duplicateLimit != null) {
            List<TrackRequest> queue = instance.getQueue();
            tracks = tracks.stream().filter(e -> queue.stream().filter(e2 -> compareTracks(e2.getTrack(), e)).count() < duplicateLimit).collect(Collectors.toList());
            if (tracks.isEmpty()) {
                throw new ValidationException(String.format("Ни один трек плейлиста не подходит под ограничение %d одинаковых треков :raised_hand:", duplicateLimit));
            }
        }

        if (tracks.isEmpty()) {
            throw new ValidationException("Указанный плейлист пуст");
        }

        if (queueLimit != null) {
            List<TrackRequest> userQueue = instance.getQueue(requestedBy);
            int availableSlots = queueLimit.intValue() - userQueue.size();
            if (availableSlots <= 0) {
                throw new ValidationException(String.format("Вы превысили лимит треков в очереди (%s) :raised_hand:", queueLimit));
            }
            if (tracks.size() > availableSlots) {
                tracks = tracks.subList(0, availableSlots);
            }
        }
        return tracks;
    }
}
