package ru.caramel.juniperbot.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.managers.AudioManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Component
@Scope("prototype")
public class GuildPlaybackManager extends AudioEventAdapter {

    @Autowired
    private Guild guild;

    @Autowired
    private AudioManager audioManager;

    @Autowired
    private AudioPlayerManager playerManager;

    private VoiceChannel channel;

    private AudioPlayer player;

    private final BlockingQueue<AudioTrack> queue = new LinkedBlockingQueue<>();

    @PostConstruct
    public void init() {
        player = playerManager.createPlayer();
        player.addListener(this);
        audioManager.setSendingHandler(new AudioPlayerSendHandler(player));
        channel = guild.getVoiceChannels().stream().findFirst().orElse(null);
    }

    public void play(AudioTrack track) {
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
        // Start the next track, regardless of if something is already playing or not. In case queue was empty, we are
        // giving null to startTrack, which is a valid argument and will simply stop the player.
        player.startTrack(queue.poll(), false);
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        // Only start the next track if the end reason is suitable for it (FINISHED or LOAD_FAILED)
        if (!endReason.mayStartNext) {
            return;
        }
        if (!queue.isEmpty()) {
            nextTrack();
            return;
        }
        if (audioManager.isConnected() || audioManager.isAttemptingToConnect()) {
            audioManager.closeAudioConnection();
        }
    }

    public Collection<AudioTrack> getQueue() {
        return Collections.unmodifiableCollection(queue);
    }
}
