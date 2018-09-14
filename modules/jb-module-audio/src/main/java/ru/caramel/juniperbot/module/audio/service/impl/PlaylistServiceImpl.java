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
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.io.MessageInput;
import com.sedmelluq.discord.lavaplayer.tools.io.MessageOutput;
import com.sedmelluq.discord.lavaplayer.track.*;
import net.dv8tion.jda.core.entities.Member;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.caramel.juniperbot.core.persistence.entity.LocalMember;
import ru.caramel.juniperbot.core.service.*;
import ru.caramel.juniperbot.module.audio.model.StoredPlaylist;
import ru.caramel.juniperbot.module.audio.model.PlaybackInstance;
import ru.caramel.juniperbot.module.audio.model.TrackRequest;
import ru.caramel.juniperbot.module.audio.persistence.entity.Playlist;
import ru.caramel.juniperbot.module.audio.persistence.entity.PlaylistItem;
import ru.caramel.juniperbot.module.audio.persistence.repository.PlaylistItemRepository;
import ru.caramel.juniperbot.module.audio.persistence.repository.PlaylistRepository;
import ru.caramel.juniperbot.module.audio.service.LavaAudioService;
import ru.caramel.juniperbot.module.audio.service.PlaylistService;
import ru.caramel.juniperbot.module.audio.utils.PlaylistUtils;

import javax.annotation.PostConstruct;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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

    @Autowired
    private LavaAudioService lavaAudioService;

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
            if (playlist != null) {
                List<PlaylistItem> items = new ArrayList<>(playlist.getItems());
                if (CollectionUtils.isNotEmpty(items)) {
                    List<AudioTrack> tracks = new ArrayList<>(items.size());
                    items.forEach(e -> {
                        try {
                            AudioTrack track = getTracksFor(manager, e);
                            if (track != null) {
                                tracks.add(track);
                            }
                        } catch (Exception ex) {
                            // fall down and ignore it
                        }
                    });
                    if (!tracks.isEmpty()) {
                        refreshStoredPlaylist(playlist, tracks);
                        return new StoredPlaylist(playlist, tracks);
                    }
                }
            }
            onError(playlist, "discord.command.audio.playlist.notFound");
        }
        return null;
    }

    private void onError(Playlist playlist, String messageCode) throws FriendlyException {
        contextService.withContext(playlist.getGuildId(), () -> {
            throw new FriendlyException(messageService.getMessage(messageCode), COMMON, null);
        });
    }

    private AudioTrack getTracksFor(DefaultAudioPlayerManager manager, PlaylistItem item) {
        if (sourceMethod == null) {
            return null;
        }

        class TrackHolder {
            AudioTrack track;
        }

        TrackHolder holder = new TrackHolder();

        if (ArrayUtils.isNotEmpty(item.getData())) {
            holder.track = decodeTrack(item.getData());
        }

        if (holder.track == null) {
            AudioLoadResultHandler handler = new AudioLoadResultHandler() {
                @Override
                public void trackLoaded(AudioTrack track) {
                    holder.track = track;
                }

                @Override
                public void playlistLoaded(AudioPlaylist playlist) {
                    // no playlist supported
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

            if (ArrayUtils.isEmpty(item.getData()) && holder.track != null) {
                item.setData(encodeTrack(holder.track));
                playlistItemRepository.save(item);
            }
        }
        return holder.track;
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
                    item.setData(encodeTrack(request.getTrack()));
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
    @Async
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
                    item.setData(encodeTrack(e.getTrack()));
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
            LOGGER.warn("[shuffle] Could not update playlist", e);
        }
    }

    private void refreshStoredPlaylist(Playlist playlist, List<AudioTrack> tracks) {
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
            LOGGER.warn("[shuffle] Could not clear playlist", e);
        }
    }

    private byte[] encodeTrack(AudioTrack track) {
        if (track == null) {
            return null;
        }
        AudioPlayerManager manager = lavaAudioService.getPlayerManager();
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            MessageOutput output = new MessageOutput(outputStream);
            manager.encodeTrack(output, track);
            return outputStream.toByteArray();
        } catch (IOException e) {
            LOGGER.warn("Could not encode track {}", track);
        }
        return null;
    }

    private AudioTrack decodeTrack(byte[] data) {
        if (ArrayUtils.isEmpty(data)) {
            return null;
        }
        AudioPlayerManager manager = lavaAudioService.getPlayerManager();
        try (ByteArrayInputStream stream = new ByteArrayInputStream(data)) {
            DecodedTrackHolder holder = manager.decodeTrack(new MessageInput(stream));
            return holder != null && holder.decodedTrack != null ? holder.decodedTrack : null;
        } catch (IOException e) {
            LOGGER.warn("Could not decode track");
        }
        return null;
    }

    private Playlist validateItems(Playlist playlist) {
        if (playlist != null && CollectionUtils.isNotEmpty(playlist.getItems()) && playlist.getItems().contains(null)) {
            playlist.setItems(playlist.getItems().stream().filter(Objects::nonNull).collect(Collectors.toList()));
            playlistRepository.save(playlist);
        }
        return playlist;
    }
}
