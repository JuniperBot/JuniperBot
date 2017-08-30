package ru.caramel.juniperbot.audio.model;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;

import java.util.concurrent.ScheduledFuture;

@Getter
@Setter
public class TrackRequest {

    private AudioTrack track;

    private final Member member;

    private final TextChannel channel;

    private ScheduledFuture<?> updaterTask;

    private Message infoMessage;

    private boolean resetMessage;

    public TrackRequest(AudioTrack track, Member member, TextChannel channel) {
        this.track = track;
        this.member = member;
        this.channel = channel;
    }

    public void reset() {
        if (track != null) {
            track = track.makeClone();
        }
    }
}
