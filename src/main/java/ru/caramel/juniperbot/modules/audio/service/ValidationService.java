/*
 * This file is part of JuniperBotJ.
 *
 * JuniperBotJ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * JuniperBotJ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with JuniperBotJ. If not, see <http://www.gnu.org/licenses/>.
 */
package ru.caramel.juniperbot.modules.audio.service;

import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import net.dv8tion.jda.core.entities.Member;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.caramel.juniperbot.modules.audio.model.TrackRequest;
import ru.caramel.juniperbot.core.model.BotContext;
import ru.caramel.juniperbot.core.model.exception.ValidationException;
import ru.caramel.juniperbot.modules.audio.persistence.entity.MusicConfig;

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

    public void validateSingle(AudioTrack track, Member requestedBy, BotContext context) throws ValidationException {
        MusicConfig config = context.getConfig().getMusicConfig();
        if (config == null) {
            context.getConfig().setMusicConfig(config = new MusicConfig());
        }
        Long queueLimit = config.getQueueLimit();
        Long durationLimit = config.getDurationLimit();
        Long duplicateLimit = config.getDuplicateLimit();
        PlaybackInstance instance = playerService.getInstance(context.getGuild());

        if (track.getInfo().isStream && !config.isStreamsEnabled()) {
            throw new ValidationException("discord.command.audio.queue.limits.streams");
        }

        if (queueLimit != null) {
            List<TrackRequest> userQueue = instance.getQueue(requestedBy);
            if (userQueue.size() >= queueLimit) {
                throw new ValidationException("discord.command.audio.queue.limits.items", queueLimit);
            }
        }
        if (duplicateLimit != null) {
            List<TrackRequest> userQueue = instance.getQueue().stream().filter(e -> compareTracks(e.getTrack(), track)).collect(Collectors.toList());
            if (userQueue.size() >= duplicateLimit) {
                throw new ValidationException("discord.command.audio.queue.limits.duplicates", duplicateLimit);
            }
        }
        if (!track.getInfo().isStream && durationLimit != null && track.getDuration() >= (durationLimit * 60000)) {
            throw new ValidationException("discord.command.audio.queue.limits.duration", durationLimit);
        }
    }

    public List<AudioTrack> filterPlaylist(AudioPlaylist playlist, Member requestedBy, BotContext context) throws ValidationException {
        MusicConfig config = context.getConfig().getMusicConfig();
        if (config == null) {
            context.getConfig().setMusicConfig(config = new MusicConfig());
        }
        Long queueLimit = config.getQueueLimit();
        Long durationLimit = config.getDurationLimit();
        Long duplicateLimit = config.getDuplicateLimit();
        PlaybackInstance instance = playerService.getInstance(context.getGuild());

        if (!Boolean.TRUE.equals(config.getPlaylistEnabled())) {
            throw new ValidationException("discord.command.audio.queue.limits.playlists");
        }

        List<AudioTrack> tracks = playlist.getTracks();
        if (!tracks.isEmpty() && !config.isStreamsEnabled()) {
            tracks = tracks.stream().filter(e -> !e.getInfo().isStream).collect(Collectors.toList());
            if (tracks.isEmpty()) {
                throw new ValidationException("discord.command.audio.queue.limits.streams");
            }
        }

        if (!tracks.isEmpty() && durationLimit != null) {
            tracks = tracks.stream().filter(e -> e.getInfo().isStream || e.getDuration() < (durationLimit * 60000)).collect(Collectors.toList());
            if (tracks.isEmpty()) {
                throw new ValidationException("discord.command.audio.queue.limits.duration.playlist", durationLimit);
            }
        }

        if (!tracks.isEmpty() && duplicateLimit != null) {
            List<TrackRequest> queue = instance.getQueue();
            tracks = tracks.stream().filter(e -> queue.stream().filter(e2 -> compareTracks(e2.getTrack(), e)).count() < duplicateLimit).collect(Collectors.toList());
            if (tracks.isEmpty()) {
                throw new ValidationException("discord.command.audio.queue.limits.duplicates.playlist", duplicateLimit);
            }
        }

        if (tracks.isEmpty()) {
            throw new ValidationException("discord.command.audio.queue.limits.playlistEmpty");
        }

        if (queueLimit != null) {
            List<TrackRequest> userQueue = instance.getQueue(requestedBy);
            int availableSlots = queueLimit.intValue() - userQueue.size();
            if (availableSlots <= 0) {
                throw new ValidationException("discord.command.audio.queue.limits.items", queueLimit);
            }
            if (tracks.size() > availableSlots) {
                tracks = tracks.subList(0, availableSlots);
            }
        }
        return tracks;
    }
}
