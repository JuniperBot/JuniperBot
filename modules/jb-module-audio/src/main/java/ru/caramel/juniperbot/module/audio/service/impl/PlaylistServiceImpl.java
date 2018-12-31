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
package ru.caramel.juniperbot.module.audio.service.impl;

import com.sedmelluq.discord.lavaplayer.track.*;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.entities.Member;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.caramel.juniperbot.core.persistence.entity.LocalMember;
import ru.caramel.juniperbot.core.service.*;
import ru.caramel.juniperbot.module.audio.model.PlaybackInstance;
import ru.caramel.juniperbot.module.audio.model.TrackRequest;
import ru.caramel.juniperbot.module.audio.persistence.entity.Playlist;
import ru.caramel.juniperbot.module.audio.persistence.entity.PlaylistItem;
import ru.caramel.juniperbot.module.audio.persistence.repository.PlaylistItemRepository;
import ru.caramel.juniperbot.module.audio.persistence.repository.PlaylistRepository;
import ru.caramel.juniperbot.module.audio.service.PlaylistService;
import ru.caramel.juniperbot.module.audio.service.handling.JbAudioPlayerManager;
import ru.caramel.juniperbot.module.audio.utils.PlaylistUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PlaylistServiceImpl implements PlaylistService {

    @Autowired
    private PlaylistRepository playlistRepository;

    @Autowired
    private PlaylistItemRepository playlistItemRepository;

    @Autowired
    private MemberService memberService;

    @Autowired
    private ApplicationContext applicationContext;

    private JbAudioPlayerManager audioPlayerManager;

    @Override
    @Transactional
    public Playlist getPlaylist(String uuid) {
        return validateItems(playlistRepository.findByUuid(uuid));
    }

    @Override
    @Transactional
    public Playlist find(Long id) {
        return validateItems(playlistRepository.findById(id).orElse(null));
    }

    @Override
    @Transactional
    public Playlist save(Playlist playlist) {
        return playlistRepository.save(playlist);
    }

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
            playlist = find(instance.getPlaylistId());
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
        LocalMember localMember = memberService.getOrCreate(member);

        synchronized (instance) {
            try {
                Playlist playlist = getPlaylist(instance);
                for (TrackRequest request : requests) {
                    PlaylistItem item = new PlaylistItem(request.getTrack(), localMember);
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
                    LocalMember member = memberService.getOrCreate(e.getMember());
                    item = new PlaylistItem(e.getTrack(), member);
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

    private Playlist validateItems(Playlist playlist) {
        if (playlist != null && CollectionUtils.isNotEmpty(playlist.getItems()) && playlist.getItems().contains(null)) {
            playlist.setItems(playlist.getItems().stream().filter(Objects::nonNull).collect(Collectors.toList()));
            playlistRepository.save(playlist);
        }
        return playlist;
    }

    private JbAudioPlayerManager getAudioPlayerManager() {
        if (audioPlayerManager == null) {
            audioPlayerManager = applicationContext.getBean(JbAudioPlayerManager.class);
        }
        return audioPlayerManager;
    }
}
