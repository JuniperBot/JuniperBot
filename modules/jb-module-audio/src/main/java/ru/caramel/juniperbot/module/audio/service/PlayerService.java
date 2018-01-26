package ru.caramel.juniperbot.module.audio.service;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.VoiceChannel;
import ru.caramel.juniperbot.core.model.exception.DiscordException;
import ru.caramel.juniperbot.module.audio.model.PlaybackInstance;
import ru.caramel.juniperbot.module.audio.model.TrackRequest;
import ru.caramel.juniperbot.module.audio.persistence.entity.MusicConfig;

import java.util.List;

public interface PlayerService {

    String ACTIVE_CONNECTIONS = "player.activeConnections";

    AudioPlayerManager getPlayerManager();

    MusicConfig getConfig(long serverId);

    MusicConfig getConfig(Guild guild);

    PlaybackInstance getInstance(Guild guild);

    void play(List<TrackRequest> requests) throws DiscordException;

    void play(TrackRequest request) throws DiscordException;

    void play(TrackRequest request, PlaybackInstance instance) throws DiscordException;

    void skipTrack(Guild guild);

    boolean isInChannel(Member member);

    VoiceChannel getChannel(Member member);

    void reconnectAll();

    VoiceChannel connectToChannel(PlaybackInstance instance, Member member) throws DiscordException;

    void monitor();

    long getActiveCount();
}
