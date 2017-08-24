package ru.caramel.juniperbot.audio.service;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import ru.caramel.juniperbot.audio.model.TrackRequest;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PlaybackHandlerService {

    @Autowired
    private ApplicationContext applicationContext;

    private Map<Long, PlaybackHandler> handlerMap = new ConcurrentHashMap<>();

    public PlaybackHandler getHandler(Guild guild) {
        long guildId = Long.parseLong(guild.getId());
        return handlerMap.computeIfAbsent(guildId,
                e -> applicationContext.getBean(PlaybackHandler.class));
    }

    public void skipTrack(Guild guild) {
        getHandler(guild).nextTrack();
    }

    public void setVolume(Guild guild, int volume) {
        getHandler(guild).setVolume(volume);
    }

    public boolean isInChannel(Guild guild, User user) {
        return getHandler(guild).isInChannel(user);
    }

    public boolean pauseTrack(Guild guild) {
        return getHandler(guild).pauseTrack();
    }

    public boolean shuffleTracks(Guild guild) {
        return getHandler(guild).shuffle();
    }

    public boolean resumeTrack(Guild guild) {
        return getHandler(guild).resumeTrack();
    }

    public boolean stop(Guild guild) {
        return getHandler(guild).stop();
    }

    public List<TrackRequest> getQueue(Guild guild) {
        return getHandler(guild).getQueue();
    }
}
