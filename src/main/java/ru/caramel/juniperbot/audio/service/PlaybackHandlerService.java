package ru.caramel.juniperbot.audio.service;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.caramel.juniperbot.audio.model.RepeatMode;
import ru.caramel.juniperbot.audio.model.TrackRequest;
import ru.caramel.juniperbot.integration.discord.DiscordClient;

import java.util.*;

@Service
public class PlaybackHandlerService {

    private final static long TIMEOUT = 180000; // 3 minutes

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private AudioMessageManager messageManager;

    @Autowired
    private DiscordClient discordClient;

    private final Map<Long, PlaybackHandler> handlerMap = new HashMap<>();

    private final Map<Long, Long> activeTime = new HashMap<>();

    public PlaybackHandler getHandler(Guild guild) {
        synchronized (handlerMap) {
            return handlerMap.computeIfAbsent(guild.getIdLong(),
                    e -> {
                        activeTime.put(e, System.currentTimeMillis());
                        return applicationContext.getBean(PlaybackHandler.class);
                    });
        }
    }

    public void skipTrack(Guild guild) {
        getHandler(guild).nextTrack();
    }

    public void setVolume(Guild guild, int volume) {
        getHandler(guild).setVolume(volume);
    }

    public boolean isInChannel(Member member) {
        return getHandler(member.getGuild()).isInChannel(member);
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

    public boolean seek(Guild guild, long position) {
        return getHandler(guild).seek(position);
    }

    public boolean setMode(Guild guild, RepeatMode mode) {
        return getHandler(guild).setMode(mode);
    }

    public TrackRequest getCurrent(Guild guild) {
        return getHandler(guild).getCurrent();
    }

    public List<TrackRequest> getQueue(Guild guild) {
        return getHandler(guild).getQueue();
    }

    @Scheduled(fixedDelay = 15000)
    public void monitor() {
        synchronized (handlerMap) {
            long currentTimeMillis = System.currentTimeMillis();

            Set<Long> inactiveIds = new HashSet<>();
            handlerMap.forEach((k, v) -> {
                long lastMillis = activeTime.computeIfAbsent(k, e -> currentTimeMillis);
                TrackRequest current = v.getCurrent();
                if (!discordClient.isConnected() || v.getChannel() != null && v.getChannel().getMembers().stream()
                        .filter(e -> !e.getUser().equals(e.getJDA().getSelfUser())).count() > 0) {
                    activeTime.put(k, currentTimeMillis);
                    return;
                }
                if (current != null && currentTimeMillis - lastMillis > TIMEOUT) {
                    v.stop();
                    messageManager.onTimeout(current.getChannel());
                    inactiveIds.add(k);
                }
            });
            inactiveIds.forEach(e -> {
                handlerMap.remove(e).stop();
                activeTime.remove(e);
            });
        }
    }
}
