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

import com.codahale.metrics.annotation.Gauge;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import lavalink.client.player.IPlayer;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.exceptions.InsufficientPermissionException;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ResourceUtils;
import ru.caramel.juniperbot.core.model.exception.DiscordException;
import ru.caramel.juniperbot.core.service.ContextService;
import ru.caramel.juniperbot.core.service.DiscordService;
import ru.caramel.juniperbot.core.support.ModuleListener;
import ru.caramel.juniperbot.module.audio.model.*;
import ru.caramel.juniperbot.module.audio.persistence.entity.MusicConfig;
import ru.caramel.juniperbot.module.audio.service.*;
import ru.caramel.juniperbot.module.audio.service.helper.AudioMessageManager;
import ru.caramel.juniperbot.module.audio.service.helper.PlayerListenerAdapter;
import ru.caramel.juniperbot.module.audio.service.helper.ValidationService;
import ru.caramel.juniperbot.module.audio.service.helper.YouTubeService;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class PlayerServiceImpl extends PlayerListenerAdapter implements PlayerService, ModuleListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerServiceImpl.class);

    private final static long TIMEOUT = 180000; // 3 minutes

    @Autowired
    private AudioMessageManager messageManager;

    @Autowired
    private DiscordService discordService;

    @Autowired
    private PlaylistService playlistService;

    @Autowired
    private MusicConfigService musicConfigService;

    @Autowired
    private ContextService contextService;

    @Autowired
    private LavaAudioService lavaAudioService;

    @Autowired
    protected ValidationService validationService;

    @Autowired
    private YouTubeService youTubeService;

    @Autowired
    @Qualifier("executor")
    private TaskExecutor taskExecutor;

    private final Map<Long, PlaybackInstance> instances = new ConcurrentHashMap<>();

    /* ========================================================
     *                  Instance Factory
     * ======================================================== */

    @Override
    public Map<Long, PlaybackInstance> getInstances() {
        return Collections.unmodifiableMap(instances);
    }

    @Override
    public PlaybackInstance getInstance(Guild guild) {
        return getInstance(guild.getIdLong(), true);
    }

    @Override
    public PlaybackInstance getInstance(long guildId, boolean create) {
        return create ? instances.computeIfAbsent(guildId, e -> {
            MusicConfig config = musicConfigService.getOrCreate(guildId);
            IPlayer player = lavaAudioService.createPlayer(String.valueOf(guildId));
            player.setVolume(config.getVoiceVolume());
            return registerInstance(new PlaybackInstance(e, player));
        }) : instances.get(guildId);
    }

    @Override
    public boolean isActive(Guild guild) {
        if (!lavaAudioService.isConnected(guild) || lavaAudioService.getConnectedChannel(guild) == null) {
            return false;
        }
        PlaybackInstance instance = getInstance(guild.getIdLong(), false);
        return instance != null && instance.getPlayer().getPlayingTrack() != null;
    }

    @Override
    public boolean isActive(PlaybackInstance instance) {
        if (instance == null) {
            return false;
        }
        Guild guild = discordService.getShardManager().getGuildById(instance.getGuildId());
        return guild != null
                && lavaAudioService.isConnected(guild)
                && lavaAudioService.getConnectedChannel(guild) != null
                && instance.getPlayer().getPlayingTrack() != null;
    }

    private void clearInstance(PlaybackInstance instance, boolean notify) {
        if (stopInstance(instance, notify)) {
            messageManager.clear(instance.getGuildId());
            instances.remove(instance.getGuildId());
            clearInstance(instance);
        }
    }

    private boolean stopInstance(PlaybackInstance instance, boolean notify) {
        if (instance != null) {
            if (notify) {
                notifyCurrentEnd(instance, AudioTrackEndReason.STOPPED);
            }
            instance.stop();
            musicConfigService.updateVolume(instance.getGuildId(), instance.getPlayer().getVolume());
            Guild guild = discordService.getShardManager().getGuildById(instance.getGuildId());
            if (guild != null) {
                lavaAudioService.closeConnection(guild);
            }
            return true;
        }
        return false;
    }

    private void notifyCurrentEnd(PlaybackInstance instance, AudioTrackEndReason endReason) {
        TrackRequest current = instance.getCurrent();
        if (current != null) {
            if (current.getEndReason() == null) {
                current.setEndReason(EndReason.getForLavaPlayer(endReason));
            }
            contextService.withContext(current.getGuild(), () -> messageManager.onTrackEnd(current));
        }
    }

    /* ========================================================
     *                 Player Event Handlers
     * ======================================================== */

    @Override
    protected void onTrackStart(PlaybackInstance instance) {
        TrackRequest request = instance.getCurrent();
        if (request != null
                && request.getTimeCode() != null
                && request.getTimeCode() > 0
                && request.getTrack().isSeekable()) {
            long seekTo = request.getTimeCode();
            if (request.getTrack().getDuration() > seekTo) {
                instance.seek(seekTo);
            }
        }
        contextService.withContext(instance.getGuildId(), () -> messageManager.onTrackStart(instance.getCurrent()));
    }

    @Override
    protected void onTrackEnd(PlaybackInstance instance, AudioTrackEndReason endReason) {
        notifyCurrentEnd(instance, endReason);
        switch (endReason) {
            case STOPPED:
            case REPLACED:
                return;
            case FINISHED:
            case LOAD_FAILED:
                if (instance.playNext()) {
                    return;
                }
                TrackRequest current = instance.getCurrent();
                if (current != null) {
                    contextService.withContext(instance.getGuildId(), () -> messageManager.onQueueEnd(current));
                }
                break;
            case CLEANUP:
                break;
        }

        // execute instance reset out of current thread
        taskExecutor.execute(() -> clearInstance(instance, false));
    }

    @Override
    protected void onTrackException(PlaybackInstance instance, FriendlyException exception) {
        LOGGER.warn("Track error", exception);
    }

    /* ========================================================
     *                       Actions
     * ======================================================== */

    @Override
    public void loadAndPlay(final TextChannel channel, final Member requestedBy, String trackUrl) {
        final Long timeCode;
        if (!ResourceUtils.isUrl(trackUrl)) {
            String result = youTubeService.searchForUrl(trackUrl);
            trackUrl = result != null ? result : trackUrl;
            timeCode = null;
        } else {
            timeCode = youTubeService.extractTimecode(trackUrl);
        }
        final String query = trackUrl;

        PlaybackInstance instance = getInstance(channel.getGuild());
        lavaAudioService.getPlayerManager().loadItemOrdered(instance, query, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                contextService.withContext(channel.getGuild(), () -> {
                    try {
                        validationService.validateSingle(track, requestedBy);
                        play(new TrackRequest(track, requestedBy, channel, timeCode));
                    } catch (DiscordException e) {
                        messageManager.onQueueError(channel, e.getMessage(), e.getArgs());
                    }
                });
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                contextService.withContext(channel.getGuild(), () -> {
                    try {
                        List<AudioTrack> tracks = validationService.filterPlaylist(playlist, requestedBy);
                        play(playlist, tracks.stream().map(e -> new TrackRequest(e, requestedBy, channel))
                                .collect(Collectors.toList()));
                    } catch (DiscordException e) {
                        messageManager.onQueueError(channel, e.getMessage(), e.getArgs());
                    }
                });
            }

            @Override
            public void noMatches() {
                contextService.withContext(channel.getGuild(), () -> messageManager.onNoMatches(channel, query));
            }

            @Override
            public void loadFailed(FriendlyException e) {
                contextService.withContext(channel.getGuild(), () ->
                        messageManager.onQueueError(channel, "discord.command.audio.error", e.getMessage()));
            }
        });
    }

    @Override
    @Transactional
    public void play(AudioPlaylist playlist, List<TrackRequest> requests) throws DiscordException {
        if (CollectionUtils.isEmpty(requests)) {
            return;
        }

        TrackRequest request = requests.get(0);
        Guild guild = request.getChannel().getGuild();
        PlaybackInstance instance = getInstance(guild);

        boolean store = true;
        if (playlist instanceof StoredPlaylist) {
            StoredPlaylist storedPlaylist = (StoredPlaylist) playlist;
            if (isActive(guild)) {
                if (Objects.equals(storedPlaylist.getPlaylistId(), instance.getPlaylistId())) {
                    throw new DiscordException("discord.command.audio.playlistInQueue");
                }
            } else if (storedPlaylist.getGuildId() == guild.getIdLong() && !isActive(guild)) {
                instance.setPlaylistId(storedPlaylist.getPlaylistId());
                instance.setPlaylistUuid(storedPlaylist.getPlaylistUuid());
                store = false;
            }
        }

        if (store) {
            playlistService.storeToPlaylist(instance, requests);
        }

        play(request, instance);
        if (requests.size() > 1) {
            requests.subList(1, requests.size()).forEach(instance::offer);
        }
    }

    @Override
    @Transactional
    public void play(TrackRequest request) throws DiscordException {
        PlaybackInstance instance = getInstance(request.getChannel().getGuild());
        playlistService.storeToPlaylist(instance, Collections.singletonList(request));
        play(request, instance);
    }

    private void play(TrackRequest request, PlaybackInstance instance) throws DiscordException {
        contextService.withContext(request.getGuild(), () -> messageManager.onTrackAdd(request, instance.getCursor() < 0));
        connectToChannel(instance, request.getMember());
        instance.play(request);
    }

    @Override
    public void skipTrack(Member member, Guild guild) {
        PlaybackInstance instance = getInstance(guild);
        // сбросим режим если принудительно вызвали следующий
        if (RepeatMode.CURRENT.equals(instance.getMode())) {
            instance.setMode(RepeatMode.NONE);
        }
        if (instance.getCurrent() != null) {
            instance.getCurrent().setEndReason(EndReason.SKIPPED);
            instance.getCurrent().setEndMember(member);
        }
        onTrackEnd(instance, AudioTrackEndReason.FINISHED);
    }

    @Override
    public boolean stop(Member member, Guild guild) {
        PlaybackInstance instance = getInstance(guild.getIdLong(), false);
        if (instance == null) {
            return false;
        }
        if (instance.getCurrent() != null) {
            instance.getCurrent().setEndReason(EndReason.STOPPED);
            instance.getCurrent().setEndMember(member);
        }
        contextService.withContextAsync(guild, () -> clearInstance(instance, true));
        return true;
    }

    @Override
    public boolean pause(Guild guild) {
        if (!isActive(guild)) {
            return false;
        }
        PlaybackInstance instance = getInstance(guild);
        if (instance.pauseTrack()) {
            contextService.withContext(instance.getGuildId(), () -> messageManager.onTrackPause(instance.getCurrent()));
            return true;
        }
        return false;
    }

    @Override
    public boolean resume(Guild guild, boolean resetMessage) {
        if (!isActive(guild)) {
            return false;
        }
        PlaybackInstance instance = getInstance(guild);
        if (instance.resumeTrack(resetMessage)) {
            contextService.withContext(instance.getGuildId(), () -> messageManager.onTrackResume(instance.getCurrent()));
            return true;
        }
        return false;
    }

    @Override
    @Transactional
    public boolean shuffle(Guild guild) {
        PlaybackInstance instance = getInstance(guild);
        boolean result = instance.shuffle();
        if (result) {
            playlistService.refreshStoredPlaylist(instance);
        }
        return result;
    }

    @Override
    @Transactional
    public TrackRequest removeByIndex(Guild guild, int index) {
        PlaybackInstance instance = getInstance(guild);
        TrackRequest result = instance.removeByIndex(index);
        if (result != null && instance.getPlaylistId() != null) {
            playlistService.refreshStoredPlaylist(instance);
        }
        return result;
    }

    /* ========================================================
     *              Voice Channel Manipulation
     * ======================================================== */

    @Override
    public VoiceChannel connectToChannel(PlaybackInstance instance, Member member) throws DiscordException {
        VoiceChannel channel = musicConfigService.getDesiredChannel(member);
        if (channel == null) {
            return null;
        }
        if (!channel.getGuild().getSelfMember().hasPermission(channel, Permission.VOICE_CONNECT)) {
            throw new DiscordException("discord.global.voice.noAccess");
        }
        try {
            lavaAudioService.openConnection(instance.getPlayer(), channel);
        } catch (InsufficientPermissionException e) {
            throw new DiscordException("discord.global.voice.noAccess", e);
        }
        return channel;
    }

    @Override
    public boolean isInChannel(Member member) {
        PlaybackInstance instance = getInstance(member.getGuild().getIdLong(), false);
        VoiceChannel channel = getChannel(member, instance);
        return channel != null && channel.getMembers().contains(member);
    }

    @Override
    public VoiceChannel getChannel(Member member) {
        PlaybackInstance instance = getInstance(member.getGuild());
        return getChannel(member, instance);
    }

    private VoiceChannel getChannel(Member member, PlaybackInstance instance) {
        return isActive(instance)
                ? lavaAudioService.getConnectedChannel(instance.getGuildId())
                : musicConfigService.getDesiredChannel(member);
    }

    /* ========================================================
     *         Player Service Monitoring and Helpers
     * ======================================================== */

    @Override
    public void onShutdown() {
        instances.values().forEach(e -> {
            if (e.getCurrent() != null) {
                e.getCurrent().setEndReason(EndReason.SHUTDOWN);
            }
            stopInstance(e, true);
        });
        lavaAudioService.shutdown();
    }

    @Scheduled(fixedDelay = 15000)
    public void monitor() {
        long currentTimeMillis = System.currentTimeMillis();

        Set<PlaybackInstance> toKill = new HashSet<>();
        instances.forEach((k, v) -> {
            long lastMillis = v.getActiveTime();
            TrackRequest current = v.getCurrent();
            if (!discordService.isConnected(v.getGuildId()) || countListeners(v) > 0) {
                v.setActiveTime(currentTimeMillis);
                return;
            }
            if (currentTimeMillis - lastMillis > TIMEOUT) {
                if (current != null) {
                    contextService.withContext(current.getGuild(), () -> messageManager.onIdle(current.getChannel()));
                }
                toKill.add(v);
                stopInstance(v, true);
            }
        });
        for (PlaybackInstance instance : toKill) {
            clearInstance(instance, true);
        }
        messageManager.monitor(instances.keySet());
    }

    private long countListeners(PlaybackInstance instance) {
        if (isActive(instance)) {
            return lavaAudioService.getConnectedChannel(instance.getGuildId()).getMembers()
                    .stream()
                    .filter(e -> !e.getUser().equals(e.getJDA().getSelfUser())).count();
        }
        return 0;
    }

    @Gauge(name = ACTIVE_CONNECTIONS, absolute = true)
    @Override
    public long getActiveCount() {
        return instances.size();
    }
}
