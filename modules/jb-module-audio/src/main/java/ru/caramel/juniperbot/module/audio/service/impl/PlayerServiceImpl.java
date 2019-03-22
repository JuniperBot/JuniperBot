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
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import lavalink.client.player.IPlayer;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.exceptions.InsufficientPermissionException;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ResourceUtils;
import ru.caramel.juniperbot.core.common.model.exception.DiscordException;
import ru.caramel.juniperbot.core.common.service.DiscordService;
import ru.caramel.juniperbot.core.event.service.ContextService;
import ru.caramel.juniperbot.core.feature.service.FeatureSetService;
import ru.caramel.juniperbot.core.support.ModuleListener;
import ru.caramel.juniperbot.module.audio.model.*;
import ru.caramel.juniperbot.module.audio.persistence.entity.MusicConfig;
import ru.caramel.juniperbot.module.audio.service.LavaAudioService;
import ru.caramel.juniperbot.module.audio.service.MusicConfigService;
import ru.caramel.juniperbot.module.audio.service.PlayerService;
import ru.caramel.juniperbot.module.audio.service.PlaylistService;
import ru.caramel.juniperbot.module.audio.service.helper.AudioMessageManager;
import ru.caramel.juniperbot.module.audio.service.helper.PlayerListenerAdapter;
import ru.caramel.juniperbot.module.audio.service.helper.ValidationService;
import ru.caramel.juniperbot.module.social.service.YouTubeService;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PlayerServiceImpl extends PlayerListenerAdapter implements PlayerService, ModuleListener {

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
    private AudioPlayerManager audioPlayerManager;

    @Autowired
    private FeatureSetService featureSetService;

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
    @Transactional
    public PlaybackInstance getInstance(Guild guild) {
        return getInstance(guild.getIdLong(), true);
    }

    @Override
    @Transactional
    public PlaybackInstance getInstance(long guildId, boolean create) {
        return create ? instances.computeIfAbsent(guildId, e -> {
            MusicConfig config = musicConfigService.getOrCreate(guildId);
            IPlayer player = lavaAudioService.createPlayer(String.valueOf(guildId));
            if (featureSetService.isAvailable(guildId)) {
                player.setVolume(config.getVoiceVolume());
            }
            return registerInstance(new PlaybackInstance(e, player));
        }) : instances.get(guildId);
    }

    @Override
    public boolean isActive(Guild guild) {
        if (!lavaAudioService.isConnected(guild) || getConnectedChannel(guild) == null) {
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
                && getConnectedChannel(guild) != null
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
            if (featureSetService.isAvailable(instance.getGuildId())) {
                musicConfigService.updateVolume(instance.getGuildId(), instance.getPlayer().getVolume());
            }
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
            contextService.withContext(current.getGuildId(), () -> messageManager.onTrackEnd(current));
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
        log.warn("Track error", exception);
        Long textChannel = null;
        if (instance.getCurrent() != null) {
            textChannel = instance.getCurrent().getChannelId();
        }
        if (textChannel == null && CollectionUtils.isNotEmpty(instance.getPlaylist())) {
            textChannel = instance.getPlaylist().get(0).getChannelId();
        }
        if (textChannel != null) {
            final long channelId = textChannel;
            contextService.withContext(instance.getGuildId(), () -> messageManager.onQueueError(channelId,
                    "discord.command.audio.remote.error", exception.getMessage()));
        }
    }

    /* ========================================================
     *                       Actions
     * ======================================================== */

    @Override
    public void loadAndPlay(final TextChannel channel, final Member requestedBy, String trackUrl) {
        final Long timeCode;
        if (!ResourceUtils.isUrl(trackUrl)) {
            String result = youTubeService.searchForUrl(trackUrl);
            trackUrl = result != null ? result : "ytsearch:" + trackUrl;
            timeCode = null;
        } else {
            timeCode = youTubeService.extractTimecode(trackUrl);
        }
        final String query = trackUrl;

        JDA jda = channel.getJDA();
        long guildId = channel.getGuild().getIdLong();
        long channelId = channel.getIdLong();
        long requestedById = requestedBy.getUser().getIdLong();

        PlaybackInstance instance = getInstance(channel.getGuild());
        audioPlayerManager.loadItemOrdered(instance, query, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                contextService.withContext(guildId, () -> {
                    try {
                        validationService.validateSingle(track, requestedBy);
                        play(TrackRequest.builder()
                                .jda(jda)
                                .track(track)
                                .guildId(guildId)
                                .memberId(requestedById)
                                .channelId(channelId)
                                .timeCode(timeCode)
                                .build());
                    } catch (DiscordException e) {
                        messageManager.onQueueError(channelId, e.getMessage(), e.getArgs());
                    }
                });
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                contextService.withContext(guildId, () -> {
                    if (playlist instanceof StoredPlaylist && !featureSetService.isAvailable(guildId)) {
                        featureSetService.sendBonusMessage(channelId, "discord.bonus.audio");
                        return;
                    }

                    try {
                        List<AudioTrack> tracks = validationService.filterPlaylist(playlist, requestedBy);
                        play(playlist, tracks.stream().map(e -> TrackRequest.builder()
                                .jda(jda)
                                .track(e)
                                .guildId(guildId)
                                .memberId(requestedById)
                                .channelId(channelId)
                                .build())
                                .collect(Collectors.toList()));
                    } catch (DiscordException e) {
                        messageManager.onQueueError(channelId, e.getMessage(), e.getArgs());
                    }
                });
            }

            @Override
            public void noMatches() {
                contextService.withContext(guildId, () -> messageManager.onNoMatches(channelId, query));
            }

            @Override
            public void loadFailed(FriendlyException e) {
                contextService.withContext(guildId, () ->
                        messageManager.onQueueError(channelId, "discord.command.audio.error", e.getMessage()));
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
        Guild guild = discordService.getShardManager().getGuildById(request.getGuildId());
        if (guild == null) {
            return;
        }
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
        Guild guild = discordService.getShardManager().getGuildById(request.getGuildId());
        if (guild == null) {
            return;
        }
        PlaybackInstance instance = getInstance(guild);
        playlistService.storeToPlaylist(instance, Collections.singletonList(request));
        play(request, instance);
    }

    private void play(TrackRequest request, PlaybackInstance instance) throws DiscordException {
        contextService.withContext(request.getGuildId(),
                () -> messageManager.onTrackAdd(request, instance));

        Member member = request.getMember();
        if (member != null) {
            connectToChannel(instance, member);
            instance.play(request);
        }
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
            instance.getCurrent().setEndMemberId(member.getUser().getIdLong());
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
            if (member != null) {
                instance.getCurrent().setEndMemberId(member.getUser().getIdLong());
            }
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
        if (!channel.getGuild().getSelfMember().hasPermission(channel,
                Permission.VOICE_CONNECT, Permission.VOICE_SPEAK)) {
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
    public VoiceChannel getConnectedChannel(Guild guild) {
        //NOTE: never use the local audio manager, since the audio connection may be remote
        // there is also no reason to look the channel up remotely from lavalink, if we have access to a real guild
        // object here, since we can use the voice state of ourselves (and lavalink 1.x is buggy in keeping up with the
        // current voice channel if the bot is moved around in the client)
        return guild.getSelfMember().getVoiceState().getChannel();
    }

    @Override
    public VoiceChannel getConnectedChannel(long guildId) {
        return getConnectedChannel(discordService.getShardManager().getGuildById(guildId));
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
                ? getConnectedChannel(instance.getGuildId())
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
    @Transactional
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
                    TextChannel channel = current.getChannel();
                    if (channel != null) {
                        contextService.withContext(current.getGuildId(), () -> messageManager.onIdle(channel));
                    }
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
            return getConnectedChannel(instance.getGuildId()).getMembers()
                    .stream()
                    .filter(e -> !e.getUser().equals(e.getJDA().getSelfUser())).count();
        }
        return 0;
    }

    @Gauge(name = ACTIVE_CONNECTIONS, absolute = true)
    @Override
    public long getActiveCount() {
        return instances.values().stream().filter(this::isActive).count();
    }
}
