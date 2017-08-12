package ru.caramel.juniperbot.audio.service;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import net.dv8tion.jda.core.audio.AudioSendHandler;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.managers.AudioManager;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ru.caramel.juniperbot.audio.model.TrackRequest;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Component
@Scope("prototype")
public class GuildPlaybackManager extends AudioEventAdapter implements AudioSendHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GuildPlaybackManager.class);

    private static Set<String> channelNames = new HashSet<>(Arrays.asList("музыка", "music"));

    @Autowired
    private Guild guild;

    @Autowired
    private AudioManager audioManager;

    @Autowired
    private AudioPlayerManager playerManager;

    @Autowired
    private MessageManager messageManager;

    private AudioPlayer player;

    private AudioFrame lastFrame;

    private TrackRequest current;

    private final BlockingQueue<TrackRequest> queue = new LinkedBlockingQueue<>();

    @PostConstruct
    public void init() {
        player = playerManager.createPlayer();
        player.addListener(this);
        audioManager.setSendingHandler(this);
    }

    private VoiceChannel getDesiredChannel() {
        VoiceChannel channel;
        for (String name : channelNames) {
            channel = guild.getVoiceChannelsByName(name, true).stream().findAny().orElse(null);
            if (channel != null) {
                return channel;
            }
        }
        return guild.getVoiceChannels().stream().findFirst().orElse(null);
    }

    public void play(List<TrackRequest> requests) {
        if (CollectionUtils.isNotEmpty(requests)) {
            play(requests.get(0));
            if (requests.size() > 1) {
                requests.subList(1, requests.size() - 1).forEach(queue::offer);
            }
        }
    }

    public void play(TrackRequest request) {
        messageManager.onTrackAdd(request, player.getPlayingTrack() == null && queue.isEmpty());
        if (!audioManager.isConnected() && !audioManager.isAttemptingToConnect()) {
            audioManager.openAudioConnection(getDesiredChannel());
        }
        // Calling startTrack with the noInterrupt set to true will start the track only if nothing is currently playing. If
        // something is playing, it returns false and does nothing. In that case the player was already playing so this
        // track goes to the queue instead.
        synchronized (queue) {
            if (player.getPlayingTrack() == null) {
                current = request;
                player.setPaused(false);
                player.playTrack(request.getTrack());
            } else {
                queue.offer(request);
            }
        }
    }

    public boolean isInChannel(User user) {
        VoiceChannel channel = audioManager.isConnected() ? audioManager.getConnectedChannel() : getDesiredChannel();
        return channel.getMembers().stream().map(Member::getUser).anyMatch(user::equals);
    }

    public void nextTrack() {
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

    public boolean shuffle() {
        synchronized (queue) {
            if (queue.isEmpty()) {
                return false;
            }
            List<TrackRequest> requests = new ArrayList<>(queue);
            Collections.shuffle(requests);
            queue.clear();
            queue.addAll(requests);
            return true;
        }
    }

    @Override
    public void onPlayerPause(AudioPlayer player) {
        if (isActive()) {
            messageManager.onTrackPause(current);
        }
    }

    @Override
    public void onPlayerResume(AudioPlayer player) {
        if (isActive()) {
            messageManager.onTrackResume(current);
        }
    }

    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
        messageManager.onTrackStart(current);
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        if (track == null) {
            return;
        }
        synchronized (queue) {
            messageManager.onTrackEnd(current);
            switch (endReason) {
                case STOPPED:
                case CLEANUP:
                    queue.clear();
                    break;
                case REPLACED:
                    return;
                case FINISHED:
                case LOAD_FAILED:
                    if (!queue.isEmpty()) {
                        // Start the next track, regardless of if something is already playing or not. In case queue was empty, we are
                        // giving null to startTrack, which is a valid argument and will simply stop the player.
                        current = queue.poll();
                        player.playTrack(current.getTrack());
                        return;
                    }
                    messageManager.onQueueEnd(current);
                    break;
            }
            player.playTrack(null);
            current = null;
        }
        if (audioManager.isConnected() || audioManager.isAttemptingToConnect()) {
            audioManager.closeAudioConnection();
        }
    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
        LOGGER.error("Track error", exception);
    }

    public List<TrackRequest> getQueue() {
        synchronized (queue) {
            List<TrackRequest> result = new ArrayList<>();
            if (current != null) {
                result.add(current);
            }
            result.addAll(queue);
            return Collections.unmodifiableList(result);
        }
    }

    @Override
    public boolean canProvide() {
        if (lastFrame == null) {
            lastFrame = player.provide();
        }
        return lastFrame != null;
    }

    @Override
    public byte[] provide20MsAudio() {
        if (lastFrame == null) {
            lastFrame = player.provide();
        }
        byte[] data = lastFrame != null ? lastFrame.data : null;
        lastFrame = null;
        return data;
    }

    @Override
    public boolean isOpus() {
        return true;
    }

    private boolean isActive() {
        return player.getPlayingTrack() != null;
    }
}
