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

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.*;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

import javax.annotation.PostConstruct;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.COMMON;

@Service
public class PlaylistServiceImpl implements PlaylistService, AudioSourceManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlaylistServiceImpl.class);

    private final static String PLAYLIST_PATTERN = "https?:\\/\\/%s/playlist/([a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12})";

    @Autowired
    private PlaylistRepository playlistRepository;

    @Autowired
    private PlaylistItemRepository playlistItemRepository;

    @Autowired
    private BrandingService brandingService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private ContextService contextService;

    @Autowired
    private ConfigService configService;

    @Autowired
    private MemberService memberService;

    private Method sourceMethod;

    @PostConstruct
    public void init() {
        try {
            sourceMethod = DefaultAudioPlayerManager.class.getDeclaredMethod("checkSourcesForItem",
                    AudioReference.class, AudioLoadResultHandler.class, boolean[].class);
            sourceMethod.setAccessible(true);
        } catch (NoSuchMethodException e) {
            LOGGER.warn("Could not get LavaPlayer init method");
        }
    }

    @Override
    @Transactional
    public AudioItem loadItem(DefaultAudioPlayerManager manager, AudioReference reference) {
        Pattern pattern = Pattern.compile(String.format(PLAYLIST_PATTERN, Pattern.quote(brandingService.getWebHost())));
        Matcher matcher = pattern.matcher(reference.identifier);
        if (matcher.find()) {
            String uuid = matcher.group(1);
            Playlist playlist = getPlaylist(uuid);
            if (playlist != null && CollectionUtils.isNotEmpty(playlist.getItems())) {
                List<AudioTrack> tracks = new ArrayList<>(playlist.getItems().size());
                playlist.getItems().forEach(e -> {
                    try {
                        List<AudioTrack> items = getTracksFor(manager, e);
                        if (CollectionUtils.isNotEmpty(items)) {
                            tracks.addAll(items);
                        }
                    } catch (Exception ex) {
                        // fall down and ignore it
                    }
                });
                if (!tracks.isEmpty()) {
                    return new BasicAudioPlaylist(uuid, tracks, null, false);
                }
            }
            onError(playlist, "discord.command.audio.playlist.notFound");
        }
        return null;
    }

    private void onError(Playlist playlist, String messageCode) throws FriendlyException {
        contextService.withContext(playlist.getGuildConfig().getGuildId(), () -> {
            throw new FriendlyException(messageService.getMessage(messageCode), COMMON, null);
        });
    }

    private List<AudioTrack> getTracksFor(DefaultAudioPlayerManager manager, PlaylistItem item) {
        if (sourceMethod == null) {
            return null;
        }

        List<AudioTrack> items = new ArrayList<>();
        AudioLoadResultHandler handler = new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                items.add(track);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                if (playlist.getTracks() != null) {
                    items.addAll(playlist.getTracks());
                }
            }

            @Override
            public void noMatches() {
                // nothing
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                // nothing
            }
        };

        if (!checkSourcesForItem(manager, new AudioReference(item.getUri(), item.getTitle()), handler)) {
            checkSourcesForItem(manager, new AudioReference(item.getIdentifier(), item.getTitle()), handler);
        }
        return items;
    }

    private boolean checkSourcesForItem(DefaultAudioPlayerManager manager, AudioReference reference, AudioLoadResultHandler resultHandler) {
        if (sourceMethod == null) {
            return false;
        }
        try {
            Object result = sourceMethod.invoke(manager, reference, resultHandler, new boolean[1]);
            return Boolean.TRUE.equals(result);
        } catch (IllegalAccessException | InvocationTargetException e) {
            LOGGER.warn("Could not invoke checkSourcesForItem of LavaPlayer");
            return false;
        }
    }

    @Override
    public AudioTrack decodeTrack(AudioTrackInfo trackInfo, DataInput input) throws IOException {
        return null;
    }

    @Override
    public boolean isTrackEncodable(AudioTrack track) {
        return true;
    }

    @Override
    public void encodeTrack(AudioTrack track, DataOutput output) throws IOException {
        // No custom values that need saving
    }

    @Override
    public void shutdown() {
        // nothing to shutdown
    }

    @Override
    public String getSourceName() {
        return "jbPlaylist";
    }

    @Override
    @Transactional(readOnly = true)
    public Playlist getPlaylist(String uuid) {
        return playlistRepository.findByUuid(uuid);
    }

    @Override
    @Transactional
    public Playlist getPlaylist(PlaybackInstance instance) {
        Playlist playlist = null;
        if (instance.getPlaylistId() != null) {
            playlist = playlistRepository.findOne(instance.getPlaylistId());
        }
        if (playlist == null) {
            playlist = new Playlist();
            playlist.setUuid(String.valueOf(UUID.randomUUID()).toLowerCase());
            playlist.setItems(new ArrayList<>());
            playlist.setDate(new Date());
            playlist.setGuildConfig(configService.getOrCreate(instance.getGuildId()));
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
        LocalMember localMember = memberService.getOrCreate(requests.get(0).getMember());

        synchronized (instance) {
            try {
                Playlist playlist = getPlaylist(instance);
                for (TrackRequest request : requests) {
                    PlaylistItem item = new PlaylistItem(request.getTrack(), localMember);
                    item.setPlaylist(playlist);
                    playlist.getItems().add(item);
                }
                playlistRepository.save(playlist);
            } catch (Exception e) {
                LOGGER.warn("[store] Could not update playlist", e);
            }
        }
    }

    @Override
    @Transactional
    public void refreshStoredPlaylist(PlaybackInstance instance) {
        try {
            Playlist playlist = getPlaylist(instance);
            List<PlaylistItem> toRemove = new ArrayList<>(playlist.getItems());
            List<PlaylistItem> newItems = new ArrayList<>(playlist.getItems().size());
            instance.getPlaylist().forEach(e -> {
                PlaylistItem item = find(playlist, e.getTrack().getInfo());
                if (item == null) {
                    LocalMember member = memberService.getOrCreate(e.getMember());
                    item = new PlaylistItem(e.getTrack(), member);
                }
                newItems.add(item);
            });
            toRemove.removeAll(newItems);
            playlist.setItems(newItems);
            playlistRepository.save(playlist);
            if (!toRemove.isEmpty()) {
                playlistItemRepository.delete(toRemove);
            }
        } catch (Exception e) {
            LOGGER.warn("[shuffle] Could not update playlist", e);
        }
    }

    private static PlaylistItem find(Playlist playlist, AudioTrackInfo info) {
        for (PlaylistItem item : playlist.getItems()) {
            if (item != null &&
                    Objects.equals(item.getTitle(), info.title) &&
                    Objects.equals(item.getAuthor(), info.author) &&
                    Objects.equals(item.getLength(), info.length) &&
                    Objects.equals(item.getIdentifier(), info.identifier) &&
                    Objects.equals(item.getUri(), info.uri)) {
                return item;
            }
        }
        return null;
    }
}
