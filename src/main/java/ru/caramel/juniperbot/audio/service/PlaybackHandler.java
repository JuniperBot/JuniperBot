package ru.caramel.juniperbot.audio.service;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import lombok.Getter;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.managers.AudioManager;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ru.caramel.juniperbot.audio.model.RepeatMode;
import ru.caramel.juniperbot.audio.model.TrackRequest;
import ru.caramel.juniperbot.integration.discord.DiscordClient;
import ru.caramel.juniperbot.persistence.entity.GuildConfig;
import ru.caramel.juniperbot.service.ConfigService;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Scope("prototype")
public class PlaybackHandler extends AudioEventAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlaybackHandler.class);

    @Autowired
    private DiscordClient discordClient;

    @Autowired
    private AudioPlayerManager playerManager;

    @Autowired
    private AudioMessageManager messageManager;

    @Autowired
    private ConfigService configService;

    @Getter
    private RepeatMode mode = RepeatMode.NONE;

    @Getter
    private AudioPlayer player;

    private AudioManager audioManager;

    private final List<TrackRequest> playlist = Collections.synchronizedList(new ArrayList<>());

    private int cursor = -1;

    private Long guildId = null;

    @PostConstruct
    public void init() {
        player = playerManager.createPlayer();
        player.addListener(this);
    }

    private VoiceChannel getDesiredChannel() {
        if (guildId == null) {
            return null;
        }
        GuildConfig config = configService.getOrCreate(guildId);
        return config.getMusicChannelId() != null
                ? discordClient.getJda().getVoiceChannelById(config.getMusicChannelId()) : null;
    }

    public VoiceChannel getChannel() {
        return audioManager != null && audioManager.isConnected() && audioManager.getConnectedChannel() != null
                ? audioManager.getConnectedChannel() : getDesiredChannel();
    }

    public void play(List<TrackRequest> requests) {
        synchronized (playlist) {
            if (CollectionUtils.isNotEmpty(requests)) {
                play(requests.get(0));
                if (requests.size() > 1) {
                    requests.subList(1, requests.size()).forEach(this::offer);
                }
            }
        }
    }

    public void play(TrackRequest request) {
        Guild guild = request.getChannel().getGuild();
        if (guildId == null) {
            guildId = guild.getIdLong();
        }
        if (audioManager == null) {
            audioManager = guild.getAudioManager();
            audioManager.setSendingHandler(new GuildAudioSendHandler(player));
        }
        messageManager.onTrackAdd(request, cursor < 0);
        if (!audioManager.isConnected() && !audioManager.isAttemptingToConnect()) {
            VoiceChannel channel = getDesiredChannel();
            if (channel == null) {
                return;
            }
            audioManager.openAudioConnection(channel);
        }
        synchronized (playlist) {
            offer(request);
            if (player.getPlayingTrack() == null) {
                mode = RepeatMode.NONE;
                cursor = 0;
                player.setPaused(false);
                player.playTrack(request.getTrack());
            }
        }
    }

    public boolean isInChannel(Member member) {
        if (guildId == null) {
            guildId = member.getGuild().getIdLong();
        }
        VoiceChannel channel = getChannel();
        return channel != null && channel.getMembers().contains(member);
    }

    public void nextTrack() {
        // сбросим режим если принудительно вызвали следующий
        if (RepeatMode.CURRENT.equals(mode)) {
            mode = RepeatMode.NONE;
        }
        onTrackEnd(player, player.getPlayingTrack(), AudioTrackEndReason.FINISHED);
    }

    public boolean pauseTrack() {
        boolean playing = isActive() && !player.isPaused();
        if (playing) {
            player.setPaused(true);
        }
        return playing;
    }

    public boolean resumeTrack() {
        boolean paused = isActive() && player.isPaused();
        if (paused) {
            player.setPaused(false);
        }
        return paused;
    }

    public boolean stop() {
        boolean active = isActive();
        if (active) {
            player.stopTrack();
        }
        return active;
    }

    public boolean setMode(RepeatMode mode) {
        boolean result = isActive();
        if (result) {
            this.mode = mode;
        }
        return result;
    }

    public void setVolume(int volume) {
        player.setVolume(volume);
    }

    public boolean shuffle() {
        synchronized (playlist) {
            if (playlist.isEmpty()) {
                return false;
            }
            Collections.shuffle(getOnGoing());
            return true;
        }
    }

    private List<TrackRequest> getPast() {
        synchronized (playlist) {
            return cursor < 0 ? Collections.emptyList() : playlist.subList(0, cursor);
        }
    }

    private List<TrackRequest> getOnGoing() {
        synchronized (playlist) {
            return cursor < 0 || cursor == playlist.size() - 1
                    ? Collections.emptyList() : playlist.subList(cursor + 1, playlist.size());
        }
    }

    @Override
    public void onPlayerPause(AudioPlayer player) {
        if (isActive()) {
            messageManager.onTrackPause(getCurrent());
        }
    }

    @Override
    public void onPlayerResume(AudioPlayer player) {
        if (isActive()) {
            messageManager.onTrackResume(getCurrent());
        }
    }

    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
        messageManager.onTrackStart(getCurrent());
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        if (track == null) {
            return;
        }
        synchronized (playlist) {
            messageManager.onTrackEnd(getCurrent());
            switch (endReason) {
                case STOPPED:
                case CLEANUP:
                    mode = RepeatMode.NONE;
                    playlist.clear();
                    cursor = -1;
                    break;
                case REPLACED:
                    return;
                case FINISHED:
                case LOAD_FAILED:
                    if (RepeatMode.CURRENT.equals(mode)) {
                        getCurrent().reset();
                        player.playTrack(getCurrent().getTrack());
                        return;
                    }
                    if (cursor < playlist.size() - 1) {
                        cursor++;
                        player.playTrack(getCurrent().getTrack());
                        return;
                    }
                    if (RepeatMode.ALL.equals(mode)) {
                        cursor = 0;
                        playlist.forEach(TrackRequest::reset);
                        player.playTrack(getCurrent().getTrack());
                        return;
                    }
                    messageManager.onQueueEnd(getCurrent());
                    break;
            }
            player.playTrack(null);
            playlist.clear();
            cursor = -1;
        }
        if (audioManager.isConnected() || audioManager.isAttemptingToConnect()) {
            audioManager.closeAudioConnection();
        }
    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
        LOGGER.error("Track error", exception);
    }

    public TrackRequest getCurrent() {
        synchronized (playlist) {
            return cursor < 0 ? null : playlist.get(cursor);
        }
    }

    public List<TrackRequest> getQueue() {
        synchronized (playlist) {
            List<TrackRequest> result = new ArrayList<>();
            if (getCurrent() != null) {
                result.add(getCurrent());
            }
            result.addAll(getOnGoing());
            return Collections.unmodifiableList(result);
        }
    }

    public List<TrackRequest> getQueue(User user) {
        synchronized (playlist) {
            return getQueue().stream().filter(e -> user.equals(e.getUser())).collect(Collectors.toList());
        }
    }

    public boolean seek(long position) {
        if (isActive() && !player.getPlayingTrack().isSeekable()) {
            return false;
        }
        player.getPlayingTrack().setPosition(position);
        return true;
    }

    private void offer(TrackRequest request) {
        request.setOwner(this);
        playlist.add(request);
    }

    public boolean isActive() {
        return audioManager != null && audioManager.isConnected()
                && audioManager.getConnectedChannel() != null && player.getPlayingTrack() != null;
    }
}
