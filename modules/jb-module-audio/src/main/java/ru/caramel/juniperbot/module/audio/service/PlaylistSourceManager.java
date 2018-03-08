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
package ru.caramel.juniperbot.module.audio.service;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.*;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.caramel.juniperbot.core.service.BrandingService;
import ru.caramel.juniperbot.core.service.ContextService;
import ru.caramel.juniperbot.core.service.MessageService;
import ru.caramel.juniperbot.module.audio.persistence.entity.Playlist;
import ru.caramel.juniperbot.module.audio.persistence.entity.PlaylistItem;

import javax.annotation.PostConstruct;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.COMMON;

@Component
public class PlaylistSourceManager implements AudioSourceManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlaylistSourceManager.class);

    private final static String PLAYLIST_PATTERN = "https?:\\/\\/%s/playlist/([a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12})";

    @Autowired
    private BrandingService brandingService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private PlayerService playerService;

    @Autowired
    private ContextService contextService;

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
            Playlist playlist = playerService.getPlaylist(uuid);
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
}
