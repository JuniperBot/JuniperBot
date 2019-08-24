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
package ru.juniperbot.module.audio.service.impl;

import com.codahale.metrics.annotation.Gauge;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import lavalink.client.io.jda.JdaLink;
import lavalink.client.player.IPlayer;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ResourceUtils;
import ru.juniperbot.common.model.exception.DiscordException;
import ru.juniperbot.common.persistence.entity.MusicConfig;
import ru.juniperbot.common.service.MusicConfigService;
import ru.juniperbot.common.service.YouTubeService;
import ru.juniperbot.common.support.ModuleListener;
import ru.juniperbot.common.worker.event.service.ContextService;
import ru.juniperbot.common.worker.feature.service.FeatureSetService;
import ru.juniperbot.common.worker.shared.service.DiscordService;
import ru.juniperbot.module.audio.model.*;
import ru.juniperbot.module.audio.service.LavaAudioService;
import ru.juniperbot.module.audio.service.PlayerService;
import ru.juniperbot.module.audio.service.StoredPlaylistService;
import ru.juniperbot.module.audio.service.helper.AudioMessageManager;
import ru.juniperbot.module.audio.service.helper.PlayerListenerAdapter;
import ru.juniperbot.module.audio.service.helper.ValidationService;

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
    private StoredPlaylistService storedPlaylistService;

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
    public PlaybackInstance getOrCreate(Guild guild) {
        return get(guild.getIdLong(), true);
    }

    @Override
    @Transactional
    public PlaybackInstance get(Guild guild) {
        return get(guild.getIdLong(), false);
    }

    @Override
    @Transactional
    public PlaybackInstance get(long guildId, boolean create) {
        if (!create) {
            return instances.get(guildId);
        }
        return instances.computeIfAbsent(guildId, e -> {
            MusicConfig config = musicConfigService.getOrCreate(guildId);
            IPlayer player = lavaAudioService.createPlayer(String.valueOf(guildId));
            if (featureSetService.isAvailable(guildId)) {
                player.setVolume(config.getVoiceVolume());
            }
            return registerInstance(new PlaybackInstance(e, player));
        });
    }

    @Override
    public boolean isActive(Guild guild) {
        return isActive(get(guild));
    }

    @Override
    public boolean isActive(PlaybackInstance instance) {
        return instance != null && instance.getPlayer().getPlayingTrack() != null;
    }

    private void clearInstance(PlaybackInstance instance, boolean notify) {
        if (stopInstance(instance, notify)) {
            messageManager.clear(instance.getGuildId());
            instances.remove(instance.getGuildId());
            clearInstance(instance);
        }
    }

    private boolean stopInstance(PlaybackInstance instance, boolean notify) {
        if (instance == null) {
            return false;
        }
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
        if (endReason.mayStartNext && featureSetService.isAvailable(instance.getGuildId())) {
            if (instance.playNext()) {
                return;
            }
            TrackRequest current = instance.getCurrent();
            if (current != null) {
                contextService.withContext(instance.getGuildId(), () -> messageManager.onQueueEnd(current));
            }
        }

        if (endReason != AudioTrackEndReason.REPLACED) {
            // execute instance reset out of current thread
            taskExecutor.execute(() -> clearInstance(instance, false));
        }
    }

    @Override
    protected void onTrackStuck(PlaybackInstance instance) {
        onTrackEnd(instance, AudioTrackEndReason.LOAD_FAILED);
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

        PlaybackInstance instance = getOrCreate(channel.getGuild());
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
                        discordService.sendBonusMessage(channelId, "discord.bonus.audio");
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
        PlaybackInstance instance = getOrCreate(guild);

        boolean store = true;
        if (playlist instanceof StoredPlaylist) {
            StoredPlaylist storedPlaylist = (StoredPlaylist) playlist;
            if (isActive(guild)) {
                if (Objects.equals(storedPlaylist.getPlaylistId(), instance.getPlaylistId())) {
                    throw new DiscordException("discord.command.audio.playlistInQueue");
                }
            } else if (storedPlaylist.getGuildId() == guild.getIdLong()) {
                instance.setPlaylistId(storedPlaylist.getPlaylistId());
                instance.setPlaylistUuid(storedPlaylist.getPlaylistUuid());
                store = false;
            }
        }

        if (store) {
            storedPlaylistService.storeToPlaylist(instance, requests);
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
        PlaybackInstance instance = getOrCreate(guild);
        storedPlaylistService.storeToPlaylist(instance, Collections.singletonList(request));
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
        PlaybackInstance instance = get(guild);
        if (instance == null) {
            return;
        }
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
        PlaybackInstance instance = get(guild);
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
        PlaybackInstance instance = get(guild);
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
        PlaybackInstance instance = get(guild);
        if (instance.resumeTrack(resetMessage)) {
            contextService.withContext(instance.getGuildId(), () -> messageManager.onTrackResume(instance.getCurrent()));
            return true;
        }
        return false;
    }

    @Override
    @Transactional
    public boolean shuffle(Guild guild) {
        PlaybackInstance instance = get(guild);
        boolean result = instance != null && instance.shuffle();
        if (result) {
            storedPlaylistService.refreshStoredPlaylist(instance);
        }
        return result;
    }

    @Override
    @Transactional
    public TrackRequest removeByIndex(Guild guild, int index) {
        PlaybackInstance instance = get(guild);
        if (instance == null) {
            return null;
        }
        TrackRequest result = instance.removeByIndex(index);
        if (result != null && instance.getPlaylistId() != null) {
            storedPlaylistService.refreshStoredPlaylist(instance);
        }
        return result;
    }

    /* ========================================================
     *              Voice Channel Manipulation
     * ======================================================== */

    @Override
    public VoiceChannel connectToChannel(PlaybackInstance instance, Member member) throws DiscordException {
        VoiceChannel channel = getDesiredChannel(member);
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
        return guild.getSelfMember().getVoiceState() != null
                ? guild.getSelfMember().getVoiceState().getChannel() : null;
    }

    @Override
    public VoiceChannel getConnectedChannel(long guildId) {
        return getConnectedChannel(discordService.getShardManager().getGuildById(guildId));
    }

    @Override
    @Transactional
    public VoiceChannel getDesiredChannel(Member member) {
        MusicConfig musicConfig = musicConfigService.get(member.getGuild());
        VoiceChannel channel = null;
        if (musicConfig != null) {
            if (musicConfig.isUserJoinEnabled()
                    && member.getVoiceState() != null
                    && member.getVoiceState().inVoiceChannel()) {
                channel = member.getVoiceState().getChannel();
            }
            if (channel == null && musicConfig.getChannelId() != null) {
                channel = member.getGuild().getVoiceChannelById(musicConfig.getChannelId());
            }
        }
        if (channel == null) {
            channel = discordService.getDefaultMusicChannel(member.getGuild().getIdLong());
        }
        return channel;
    }

    @Override
    public boolean isInChannel(Member member) {
        VoiceChannel channel = getChannel(member, get(member.getGuild()));
        return channel != null && channel.getMembers().contains(member);
    }

    @Override
    @Transactional
    public boolean hasAccess(Member member) {
        MusicConfig config = musicConfigService.get(member.getGuild());
        return config == null
                || CollectionUtils.isEmpty(config.getRoles())
                || member.isOwner()
                || member.hasPermission(Permission.ADMINISTRATOR)
                || member.getRoles().stream().anyMatch(e -> config.getRoles().contains(e.getIdLong()));
    }

    @Override
    public VoiceChannel getChannel(Member member) {
        PlaybackInstance instance = get(member.getGuild());
        return getChannel(member, instance);
    }

    private VoiceChannel getChannel(Member member, PlaybackInstance instance) {
        return isActive(instance)
                ? getConnectedChannel(instance.getGuildId())
                : getDesiredChannel(member);
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

        if (lavaAudioService.getLavaLink() != null) {
            List<JdaLink> links = new ArrayList<>(lavaAudioService.getLavaLink().getLinks());
            links.forEach(link -> {
                PlaybackInstance instance = get(link.getGuildIdLong(), false);
                if (!isActive(instance)) {
                    link.destroy();
                }
            });
        }
    }

    private long countListeners(PlaybackInstance instance) {
        if (!isActive(instance)) {
            return 0;
        }
        VoiceChannel channel = getConnectedChannel(instance.getGuildId());
        if (channel == null || channel.getGuild().getSelfMember().getVoiceState().isGuildMuted()) {
            return 0;
        }
        return channel.getMembers()
                .stream()
                .filter(e -> !e.getUser().isBot() && !e.getVoiceState().isGuildDeafened())
                .count();
    }

    @Gauge(name = ACTIVE_CONNECTIONS, absolute = true)
    @Override
    public long getActiveCount() {
        return instances.values().stream().filter(this::isActive).count();
    }
}
