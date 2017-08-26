package ru.caramel.juniperbot.audio.model;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import ru.caramel.juniperbot.audio.service.PlaybackHandler;

import java.util.concurrent.ScheduledFuture;

@Getter
@Setter
public class TrackRequest {

    private AudioTrack track;

    private final User user;

    private final TextChannel channel;

    private ScheduledFuture<?> updaterTask;

    private Message infoMessage;

    private PlaybackHandler owner;

    private boolean resetMessage;

    public TrackRequest(AudioTrack track, User user, TextChannel channel) {
        this.track = track;
        this.user = user;
        this.channel = channel;
    }

    public void reset() {
        if (track != null) {
            track = track.makeClone();
        }
    }
}
