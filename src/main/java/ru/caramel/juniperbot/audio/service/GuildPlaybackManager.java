package ru.caramel.juniperbot.audio.service;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import net.dv8tion.jda.core.audio.AudioSendHandler;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.managers.AudioManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

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

    private VoiceChannel channel;

    private AudioPlayer player;

    private AudioFrame lastFrame;

    private final BlockingQueue<AudioTrack> queue = new LinkedBlockingQueue<>();

    @PostConstruct
    public void init() {
        player = playerManager.createPlayer();
        player.addListener(this);
        audioManager.setSendingHandler(this);
        channel = getTargetChannel();
    }

    private VoiceChannel getTargetChannel() {
        VoiceChannel channel;
        for (String name : channelNames) {
            channel = guild.getVoiceChannelsByName(name, true).stream().findAny().orElse(null);
            if (channel != null) {
                return channel;
            }
        }
        return guild.getVoiceChannels().stream().findFirst().orElse(null);
    }

    public void play(AudioTrack track, TextChannel sourceChannel, User user) {
        messageManager.onTrackAdd(track, sourceChannel, user, player.getPlayingTrack() == null && queue.isEmpty());
        if (channel != null && !audioManager.isConnected() && !audioManager.isAttemptingToConnect()) {
            audioManager.openAudioConnection(channel);
        }
        // Calling startTrack with the noInterrupt set to true will start the track only if nothing is currently playing. If
        // something is playing, it returns false and does nothing. In that case the player was already playing so this
        // track goes to the queue instead.
        if (!player.startTrack(track, true)) {
            queue.offer(track);
        }
    }

    public void nextTrack() {
        onTrackEnd(player, player.getPlayingTrack(), AudioTrackEndReason.FINISHED);
    }

    public void onTrackStart(AudioPlayer player, AudioTrack track) {
        messageManager.onTrackStart(track);
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        if (track == null) {
            return;
        }
        messageManager.onTrackEnd(track);
        // Only start the next track if the end reason is suitable for it (FINISHED or LOAD_FAILED)
        if (!endReason.mayStartNext) {
            return;
        }
        if (!queue.isEmpty()) {
            // Start the next track, regardless of if something is already playing or not. In case queue was empty, we are
            // giving null to startTrack, which is a valid argument and will simply stop the player.
            player.startTrack(queue.poll(), false);
            return;
        }
        player.startTrack(null, false);
        messageManager.onQueueEnd();
        if (audioManager.isConnected() || audioManager.isAttemptingToConnect()) {
            audioManager.closeAudioConnection();
        }
    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
        LOGGER.error("Track error", exception);
    }

    public Collection<AudioTrack> getQueue() {
        return Collections.unmodifiableCollection(queue);
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
}
