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
package ru.juniperbot.module.audio.service.impl;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Member;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.juniperbot.common.persistence.entity.LocalMember;
import ru.juniperbot.common.persistence.entity.Playlist;
import ru.juniperbot.common.persistence.entity.PlaylistItem;
import ru.juniperbot.common.persistence.repository.PlaylistItemRepository;
import ru.juniperbot.common.persistence.repository.PlaylistRepository;
import ru.juniperbot.common.service.PlaylistService;
import ru.juniperbot.common.worker.shared.service.DiscordEntityAccessor;
import ru.juniperbot.module.audio.model.PlaybackInstance;
import ru.juniperbot.module.audio.model.TrackData;
import ru.juniperbot.module.audio.model.TrackRequest;
import ru.juniperbot.module.audio.service.StoredPlaylistService;
import ru.juniperbot.module.audio.service.handling.JbAudioPlayerManager;
import ru.juniperbot.module.audio.utils.PlaylistUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class StoredPlaylistServiceImpl implements StoredPlaylistService {

    @Autowired
    private PlaylistRepository playlistRepository;

    @Autowired
    private PlaylistItemRepository playlistItemRepository;

    @Autowired
    private DiscordEntityAccessor entityAccessor;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private PlaylistService playlistService;

    private JbAudioPlayerManager audioPlayerManager;

    @Override
    @Transactional
    public PlaylistItem save(PlaylistItem playlistItem) {
        return playlistItemRepository.save(playlistItem);
    }

    @Override
    @Transactional
    public Playlist getPlaylist(PlaybackInstance instance) {
        Playlist playlist = null;
        if (instance.getPlaylistId() != null) {
            playlist = playlistService.find(instance.getPlaylistId());
        }
        if (playlist == null) {
            playlist = new Playlist();
            playlist.setUuid(String.valueOf(UUID.randomUUID()).toLowerCase());
            playlist.setItems(new ArrayList<>());
            playlist.setDate(new Date());
            playlist.setGuildId(instance.getGuildId());
            playlistRepository.save(playlist);
            instance.setPlaylistId(playlist.getId());
            instance.setPlaylistUuid(playlist.getUuid());
        }
        return playlist;
    }

    @Override
    @Transactional
    public void storeToPlaylist(PlaybackInstance instance, List<TrackRequest> requests) {
        if (CollectionUtils.isEmpty(requests)) {
            return;
        }
        Member member = requests.stream()
                .map(TrackRequest::getMember)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        if (member == null) {
            return;
        }
        LocalMember localMember = entityAccessor.getOrCreate(member);

        synchronized (instance) {
            try {
                Playlist playlist = getPlaylist(instance);
                for (TrackRequest request : requests) {
                    PlaylistItem item = createItem(request.getTrack(), localMember);
                    item.setPlaylist(playlist);
                    item.setData(getAudioPlayerManager().encodeTrack(request.getTrack()));
                    playlist.getItems().add(item);
                }
                playlistRepository.save(playlist);
            } catch (Exception e) {
                log.warn("[store] Could not update playlist", e);
            }
        }
    }

    private PlaylistItem createItem(AudioTrack track, LocalMember member) {
        AudioTrackInfo info = PlaylistUtils.getNormalized(track.getInfo());
        PlaylistItem playlistItem = new PlaylistItem(member);
        playlistItem.setType(track.getClass().getSimpleName());
        playlistItem.setTitle(info.title);
        playlistItem.setAuthor(info.author);
        playlistItem.setIdentifier(info.identifier);
        playlistItem.setUri(info.uri);
        playlistItem.setLength(info.length);
        playlistItem.setStream(info.isStream);
        playlistItem.setArtworkUri(TrackData.getArtwork(track));
        return playlistItem;
    }

    @Async
    @Transactional
    @Override
    public void refreshStoredPlaylist(PlaybackInstance instance) {
        try {
            Playlist playlist = getPlaylist(instance);
            List<PlaylistItem> toRemove = new ArrayList<>(playlist.getItems());
            List<PlaylistItem> newItems = new ArrayList<>(playlist.getItems().size());
            instance.getPlaylist().forEach(e -> {
                PlaylistItem item = PlaylistUtils.find(playlist, e.getTrack().getInfo());
                if (item == null) {
                    LocalMember member = entityAccessor.getOrCreate(e.getMember());
                    item = createItem(e.getTrack(), member);
                    item.setPlaylist(playlist);
                }
                if (ArrayUtils.isEmpty(item.getData())) {
                    item.setData(getAudioPlayerManager().encodeTrack(e.getTrack()));
                }
                newItems.add(item);
            });
            toRemove.removeAll(newItems);
            playlist.setItems(newItems);
            playlistRepository.save(playlist);
            if (!toRemove.isEmpty()) {
                playlistItemRepository.deleteAll(toRemove);
            }
        } catch (Exception e) {
            log.warn("[shuffle] Could not update playlist", e);
        }
    }

    @Transactional
    @Override
    public void refreshStoredPlaylist(Playlist playlist, List<AudioTrack> tracks) {
        try {
            List<PlaylistItem> toRemove = new ArrayList<>(playlist.getItems());
            List<PlaylistItem> existentItems = tracks.stream()
                    .map(e -> PlaylistUtils.find(playlist, e.getInfo()))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            toRemove.removeAll(existentItems);
            playlist.setItems(existentItems);
            playlistRepository.save(playlist);
            if (!toRemove.isEmpty()) {
                playlistItemRepository.deleteAll(toRemove.stream().filter(Objects::nonNull).collect(Collectors.toList()));
            }
        } catch (Exception e) {
            log.warn("[shuffle] Could not clear playlist", e);
        }
    }

    private JbAudioPlayerManager getAudioPlayerManager() {
        if (audioPlayerManager == null) {
            audioPlayerManager = applicationContext.getBean(JbAudioPlayerManager.class);
        }
        return audioPlayerManager;
    }
}
