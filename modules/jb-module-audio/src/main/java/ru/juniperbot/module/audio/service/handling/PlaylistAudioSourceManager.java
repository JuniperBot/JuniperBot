/*
 * This file is part of JuniperBot.
 *
 * JuniperBot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * JuniperBot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with JuniperBot. If not, see <http://www.gnu.org/licenses/>.
 */
package ru.juniperbot.module.audio.service.handling;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.juniperbot.common.configuration.CommonProperties;
import ru.juniperbot.common.persistence.entity.Playlist;
import ru.juniperbot.common.persistence.entity.PlaylistItem;
import ru.juniperbot.common.service.PlaylistService;
import ru.juniperbot.common.worker.event.service.ContextService;
import ru.juniperbot.common.worker.message.service.MessageService;
import ru.juniperbot.module.audio.model.StoredPlaylist;
import ru.juniperbot.module.audio.model.TrackData;
import ru.juniperbot.module.audio.service.StoredPlaylistService;

import javax.annotation.PostConstruct;
import java.io.DataInput;
import java.io.DataOutput;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.COMMON;

@Slf4j
@Component
public class PlaylistAudioSourceManager implements AudioSourceManager {

    private final static String PLAYLIST_PATTERN = "%s/playlist/([a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12})";

    @Autowired
    private CommonProperties commonProperties;

    @Autowired
    private MessageService messageService;

    @Autowired
    private ContextService contextService;

    @Autowired
    private PlaylistService playlistService;

    @Autowired
    private StoredPlaylistService storedPlaylistService;

    @Autowired
    private ApplicationContext applicationContext;

    private JbAudioPlayerManager audioPlayerManager;

    private Method sourceMethod;

    private Map<String, Pattern> patternMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        try {
            sourceMethod = DefaultAudioPlayerManager.class.getDeclaredMethod("checkSourcesForItem",
                    AudioReference.class, AudioLoadResultHandler.class, boolean[].class);
            sourceMethod.setAccessible(true);
        } catch (NoSuchMethodException e) {
            log.warn("Could not get LavaPlayer init method");
        }
    }

    @Override
    @Transactional
    public AudioItem loadItem(DefaultAudioPlayerManager manager, AudioReference reference) {
        String uuid = extractUUID(reference.identifier);
        if (uuid == null) {
            return null;
        }
        Playlist playlist = playlistService.getPlaylist(uuid);
        if (playlist == null) {
            return null;
        }
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
                storedPlaylistService.refreshStoredPlaylist(playlist, tracks);
                return new StoredPlaylist(playlist, tracks);
            }
        }
        onError(playlist, "discord.command.audio.playlist.notFound");
        return null;
    }

    private String extractUUID(String identifier) {
        for (String host : commonProperties.getBranding().getWebsiteAliases()) {
            Pattern pattern = patternMap.computeIfAbsent(host, e -> {
                e = e.replace("https://", "")
                        .replace("http://", "");
                return Pattern.compile(String.format(PLAYLIST_PATTERN, e));
            });
            Matcher matcher = pattern.matcher(identifier);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return null;
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
            holder.track = getAudioPlayerManager().decodeTrack(item.getData());
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
                item.setData(getAudioPlayerManager().encodeTrack(holder.track));
                playlistService.save(item);
            }
        }
        TrackData.setPlaylistItem(holder.track, item);
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
            log.warn("Could not invoke checkSourcesForItem of LavaPlayer");
            return false;
        }
    }

    private void onError(Playlist playlist, String messageCode) throws FriendlyException {
        contextService.withContext(playlist.getGuildId(), () -> {
            throw new FriendlyException(messageService.getMessage(messageCode), COMMON, null);
        });
    }

    private JbAudioPlayerManager getAudioPlayerManager() {
        if (audioPlayerManager == null) {
            audioPlayerManager = applicationContext.getBean(JbAudioPlayerManager.class);
        }
        return audioPlayerManager;
    }

    @Override
    public AudioTrack decodeTrack(AudioTrackInfo trackInfo, DataInput input) {
        return null;
    }

    @Override
    public String getSourceName() {
        return "jbPlaylist";
    }

    @Override
    public boolean isTrackEncodable(AudioTrack track) {
        return true;
    }

    @Override
    public void encodeTrack(AudioTrack track, DataOutput output) {
        // No custom values that need saving
    }

    @Override
    public void shutdown() {
        // nothing to shutdown
    }
}
