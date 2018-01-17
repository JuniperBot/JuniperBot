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

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import lombok.Getter;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.VoiceChannel;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.caramel.juniperbot.core.model.exception.DiscordException;
import ru.caramel.juniperbot.core.persistence.entity.GuildConfig;
import ru.caramel.juniperbot.core.service.ConfigService;
import ru.caramel.juniperbot.core.service.DiscordService;
import ru.caramel.juniperbot.module.audio.model.PlaybackInstance;
import ru.caramel.juniperbot.module.audio.model.RepeatMode;
import ru.caramel.juniperbot.module.audio.model.TrackRequest;
import ru.caramel.juniperbot.module.audio.persistence.entity.MusicConfig;
import ru.caramel.juniperbot.module.audio.persistence.repository.MusicConfigRepository;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PlayerServiceImpl extends AudioEventAdapter implements PlayerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerServiceImpl.class);

    private final static long TIMEOUT = 180000; // 3 minutes

    @Autowired
    private AudioMessageManager messageManager;

    @Autowired
    private DiscordService discordService;

    @Autowired
    private ConfigService configService;

    @Autowired
    private MusicConfigRepository musicConfigRepository;

    @Autowired
    @Qualifier("executor")
    private TaskExecutor taskExecutor;

    @Getter
    private AudioPlayerManager playerManager;

    private final Map<Long, PlaybackInstance> instances = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        playerManager = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerRemoteSources(playerManager);
        AudioSourceManagers.registerLocalSource(playerManager);
    }

    @PreDestroy
    public void destroy() {
        instances.values().forEach(PlaybackInstance::stop);
        playerManager.shutdown();
    }

    @Override
    @Transactional
    public MusicConfig getConfig(long serverId) {
        MusicConfig config = musicConfigRepository.findByGuildId(serverId);
        if (config == null) {
            GuildConfig guildConfig = configService.getOrCreate(serverId);
            config = new MusicConfig();
            config.setGuildConfig(guildConfig);
            musicConfigRepository.save(config);
        }
        return config;
    }

    @Override
    @Transactional
    public MusicConfig getConfig(Guild guild) {
        return getConfig(guild.getIdLong());
    }

    @Override
    public PlaybackInstance getInstance(Guild guild) {
        return instances.computeIfAbsent(guild.getIdLong(), e -> {
            AudioPlayer player = playerManager.createPlayer();
            player.addListener(this);
            return new PlaybackInstance(player);
        });
    }

    @Override
    public void play(List<TrackRequest> requests) throws DiscordException {
        if (CollectionUtils.isEmpty(requests)) {
            return;
        }
        TrackRequest request = requests.get(0);
        PlaybackInstance instance = getInstance(request.getChannel().getGuild());
        play(request, instance);
        if (requests.size() > 1) {
            requests.subList(1, requests.size()).forEach(instance::offer);
        }
    }

    @Override
    public void play(TrackRequest request) throws DiscordException {
        PlaybackInstance instance = getInstance(request.getChannel().getGuild());
        play(request, instance);
    }

    @Override
    public void play(TrackRequest request, PlaybackInstance instance) throws DiscordException {
        messageManager.onTrackAdd(request, instance.getCursor() < 0);
        connectToChannel(instance, request.getMember());
        instance.play(request);
    }

    @Override
    public VoiceChannel connectToChannel(PlaybackInstance instance, Member member) throws DiscordException {
        VoiceChannel channel = getDesiredChannel(member);
        if (channel == null) {
            return null;
        }
        if (!channel.getGuild().getSelfMember().hasPermission(channel, Permission.VOICE_CONNECT)) {
            throw new DiscordException("discord.global.voice.noAccess");
        }
        instance.openAudioConnection(channel);
        return channel;
    }

    @Override
    public void reconnectAll() {
        instances.forEach((k, v) -> {
            if (v.getCurrent() != null) {
                try {
                    connectToChannel(v, v.getCurrent().getMember());
                } catch (DiscordException e) {
                    // fall down
                }
            }
        });
    }

    @Override
    public void skipTrack(Guild guild) {
        PlaybackInstance instance = getInstance(guild);
        // сбросим режим если принудительно вызвали следующий
        if (RepeatMode.CURRENT.equals(instance.getMode())) {
            instance.setMode(RepeatMode.NONE);
        }
        onTrackEnd(instance.getPlayer(), instance.getPlayer().getPlayingTrack(), AudioTrackEndReason.FINISHED);
    }

    @Override
    public boolean isInChannel(Member member) {
        PlaybackInstance instance = getInstance(member.getGuild());
        VoiceChannel channel = getChannel(member, instance);
        return channel != null && channel.getMembers().contains(member);
    }

    private VoiceChannel getDesiredChannel(Member member) {
        MusicConfig musicConfig = getConfig(member.getGuild());
        VoiceChannel channel = null;
        if (musicConfig != null) {
            if (musicConfig.isUserJoinEnabled() && member.getVoiceState().inVoiceChannel()) {
                channel = member.getVoiceState().getChannel();
            }
            if (channel == null && musicConfig.getChannelId() != null) {
                channel = discordService.getJda().getVoiceChannelById(musicConfig.getChannelId());
            }
        }
        if (channel == null) {
            channel = discordService.getDefaultMusicChannel(member.getGuild().getIdLong());
        }
        return channel;
    }

    @Override
    public VoiceChannel getChannel(Member member) {
        PlaybackInstance instance = getInstance(member.getGuild());
        return getChannel(member, instance);
    }

    private VoiceChannel getChannel(Member member, PlaybackInstance instance) {
        return instance.isActive() ? instance.getAudioManager().getConnectedChannel() : getDesiredChannel(member);
    }

    private long countListeners(PlaybackInstance instance) {
        if (instance.isActive()) {
            return instance.getAudioManager().getConnectedChannel().getMembers()
                    .stream()
                    .filter(e -> !e.getUser().equals(e.getJDA().getSelfUser())).count();
        }
        return 0;
    }

    @Scheduled(fixedDelay = 15000)
    public void monitor() {
        long currentTimeMillis = System.currentTimeMillis();

        Set<Long> inactiveIds = new HashSet<>();
        instances.forEach((k, v) -> {
            long lastMillis = v.getActiveTime();
            TrackRequest current = v.getCurrent();
            if (!discordService.isConnected() || countListeners(v) > 0) {
                v.setActiveTime(currentTimeMillis);
                return;
            }
            if (current != null && currentTimeMillis - lastMillis > TIMEOUT) {
                messageManager.onIdle(current.getChannel());
                v.stop();
                inactiveIds.add(k);
            }
        });
        inactiveIds.forEach(instances::remove);
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        if (track == null) {
            return;
        }
        PlaybackInstance instance = track.getUserData(PlaybackInstance.class);
        if (instance == null) {
            return;
        }
        TrackRequest current = instance.getCurrent();
        if (current != null) {
            messageManager.onTrackEnd(current);
        }
        switch (endReason) {
            case STOPPED:
            case CLEANUP:
                break;
            case REPLACED:
                return;
            case FINISHED:
            case LOAD_FAILED:
                if (instance.playNext()) {
                    return;
                }
                if (current != null) {
                    messageManager.onQueueEnd(current);
                }
                break;
        }
        // execute instance reset out of current thread
        taskExecutor.execute(instance::reset);
    }

    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
        PlaybackInstance instance = track.getUserData(PlaybackInstance.class);
        messageManager.onTrackStart(instance.getCurrent());
    }

    @Override
    public void onPlayerPause(AudioPlayer player) {
        PlaybackInstance instance = player.getPlayingTrack().getUserData(PlaybackInstance.class);
        if (instance.isActive()) {
            messageManager.onTrackPause(instance.getCurrent());
        }
    }

    @Override
    public void onPlayerResume(AudioPlayer player) {
        AudioTrack track = player.getPlayingTrack();
        if (track != null) {
            PlaybackInstance instance = track.getUserData(PlaybackInstance.class);
            if (instance.isActive()) {
                messageManager.onTrackResume(instance.getCurrent());
            }
        }
    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
        LOGGER.error("Track error", exception);
    }
}