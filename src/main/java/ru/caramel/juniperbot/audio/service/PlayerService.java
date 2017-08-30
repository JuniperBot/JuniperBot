package ru.caramel.juniperbot.audio.service;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import lombok.Getter;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.VoiceChannel;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.caramel.juniperbot.audio.model.RepeatMode;
import ru.caramel.juniperbot.audio.model.TrackRequest;
import ru.caramel.juniperbot.integration.discord.DiscordClient;
import ru.caramel.juniperbot.persistence.entity.GuildConfig;
import ru.caramel.juniperbot.service.ConfigService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PlayerService extends AudioEventAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerService.class);

    private final static long TIMEOUT = 180000; // 3 minutes

    @Autowired
    private AudioMessageManager messageManager;

    @Autowired
    private DiscordClient discordClient;

    @Autowired
    private ConfigService configService;

    @Getter
    private AudioPlayerManager playerManager;

    private final Map<Guild, PlaybackInstance> instances = new ConcurrentHashMap<>();

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

    public PlaybackInstance getInstance(Guild guild) {
        return instances.computeIfAbsent(guild, e -> {
            AudioPlayer player = playerManager.createPlayer();
            player.addListener(this);
            return new PlaybackInstance(player, e);
        });
    }

    public void play(List<TrackRequest> requests) {
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

    public void play(TrackRequest request) {
        PlaybackInstance instance = getInstance(request.getChannel().getGuild());
        play(request, instance);
    }

    public void play(TrackRequest request, PlaybackInstance instance) {
        messageManager.onTrackAdd(request, instance.getCursor() < 0);
        if (!instance.isConnected()) {
            VoiceChannel channel = getDesiredChannel(instance.getGuild());
            if (channel == null) {
                return;
            }
            instance.openAudioConnection(channel);
        }
        instance.play(request);
    }

    public void skipTrack(Guild guild) {
        PlaybackInstance instance = getInstance(guild);
        // сбросим режим если принудительно вызвали следующий
        if (RepeatMode.CURRENT.equals(instance.getMode())) {
            instance.setMode(RepeatMode.NONE);
        }
        onTrackEnd(instance.getPlayer(), instance.getPlayer().getPlayingTrack(), AudioTrackEndReason.FINISHED);
    }

    public boolean isInChannel(Member member) {
        VoiceChannel channel = getChannel(member.getGuild());
        return channel != null && channel.getMembers().contains(member);
    }

    private VoiceChannel getDesiredChannel(Guild guild) {
        GuildConfig config = configService.getOrCreate(guild.getIdLong());
        return config.getMusicChannelId() != null
                ? discordClient.getJda().getVoiceChannelById(config.getMusicChannelId()) : null;
    }

    public VoiceChannel getChannel(Guild guild) {
        return getChannel(getInstance(guild));
    }

    public VoiceChannel getChannel(PlaybackInstance instance) {
        return instance.isActive() ? instance.getAudioManager().getConnectedChannel() : getDesiredChannel(instance.getGuild());
    }

    @Scheduled(fixedDelay = 15000)
    public void monitor() {
        long currentTimeMillis = System.currentTimeMillis();

        Set<Guild> inactiveIds = new HashSet<>();
        instances.forEach((k, v) -> {
            long lastMillis = v.getActiveTime();
            TrackRequest current = v.getCurrent();
            if (!discordClient.isConnected() || getChannel(v) != null && getChannel(v).getMembers().stream()
                    .filter(e -> !e.getUser().equals(e.getJDA().getSelfUser())).count() > 0) {
                v.setActiveTime(currentTimeMillis);
                return;
            }
            if (current != null && currentTimeMillis - lastMillis > TIMEOUT) {
                messageManager.onTimeout(current.getChannel());
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
        TrackRequest current = instance.getCurrent();
        messageManager.onTrackEnd(current);
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
                messageManager.onQueueEnd(current);
                break;
        }
        instance.reset();
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
        PlaybackInstance instance = player.getPlayingTrack().getUserData(PlaybackInstance.class);
        if (instance.isActive()) {
            messageManager.onTrackResume(instance.getCurrent());
        }
    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
        LOGGER.error("Track error", exception);
    }
}
