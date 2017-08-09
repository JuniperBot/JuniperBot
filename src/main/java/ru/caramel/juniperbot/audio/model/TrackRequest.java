package ru.caramel.juniperbot.audio.model;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;

import java.util.concurrent.ScheduledFuture;

public class TrackRequest {

    @Getter
    private final AudioTrack track;

    @Getter
    private final User user;

    @Getter
    private final TextChannel channel;

    @Getter
    @Setter
    private ScheduledFuture<?> updaterTask;

    @Getter
    @Setter
    private Message infoMessage;

    public TrackRequest(AudioTrack track, User user, TextChannel channel) {
        this.track = track;
        this.user = user;
        this.channel = channel;
    }
}
