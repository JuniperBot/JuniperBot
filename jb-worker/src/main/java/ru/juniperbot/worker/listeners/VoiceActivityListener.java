/*
 * This file is part of JuniperBot.
 *
 * JuniperBot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * JuniperBot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with JuniperBot. If not, see <http://www.gnu.org/licenses/>.
 */
package ru.juniperbot.worker.listeners;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.guild.voice.*;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import ru.juniperbot.common.persistence.entity.RankingConfig;
import ru.juniperbot.common.service.RankingConfigService;
import ru.juniperbot.common.worker.event.DiscordEvent;
import ru.juniperbot.common.worker.event.listeners.DiscordEventListener;
import ru.juniperbot.module.ranking.service.RankingService;
import ru.juniperbot.module.ranking.utils.GuildVoiceActivityTracker;
import ru.juniperbot.module.ranking.utils.VoiceActivityTracker;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Slf4j
@DiscordEvent(priority = 0)
public class VoiceActivityListener extends DiscordEventListener {

    @Autowired
    private RankingConfigService rankingConfigService;

    @Autowired
    private RankingService rankingService;

    private Cache</* Guild Id */Long, GuildVoiceActivityTracker> trackers = CacheBuilder.newBuilder()
            .concurrencyLevel(4)
            .expireAfterAccess(10, TimeUnit.DAYS)
            .build();

    @Override
    public void onGuildVoiceJoin(GuildVoiceJoinEvent event) {
        if (!event.getMember().getUser().isBot() && isChannelAllowed(event.getChannelJoined())) {
            startRecord(event.getChannelJoined(), event.getMember());
        }
    }

    @Override
    public void onGuildVoiceMove(GuildVoiceMoveEvent event) {
        Member member = event.getMember();
        if (member.getUser().isBot()) {
            return;
        }
        if (isChannelAllowed(event.getChannelLeft())) {
            stopRecord(event.getChannelLeft(), member);
        }
        if (isChannelAllowed(event.getChannelJoined())) {
            startRecord(event.getChannelJoined(), member);
        }
    }

    @Override
    public void onGuildVoiceLeave(GuildVoiceLeaveEvent event) {
        if (!event.getMember().getUser().isBot() && isChannelAllowed(event.getChannelLeft())) {
            stopRecord(event.getChannelLeft(), event.getMember());
        }
    }

    @Override
    public void onGuildVoiceMute(GuildVoiceMuteEvent event) {
        if (!event.getMember().getUser().isBot()) {
            updateFreeze(event.getMember(), event.getVoiceState());
        }
    }

    @Override
    public void onGuildVoiceSuppress(GuildVoiceSuppressEvent event) {
        if (!event.getMember().getUser().isBot()) {
            updateFreeze(event.getMember(), event.getVoiceState());
        }
    }

    private void startRecord(VoiceChannel channel, Member member) {
        try {
            GuildVoiceActivityTracker tracker = trackers.get(channel.getGuild().getIdLong(),
                    () -> new GuildVoiceActivityTracker(rankingConfigService, channel.getGuild().getIdLong()));
            tracker.add(channel, member, isFrozen(member));
        } catch (ExecutionException e) {
            log.warn("Can't start record for guild [{}] with member [{}]", channel.getGuild(), member, e);
        }
    }

    private void stopRecord(VoiceChannel channel, Member member) {
        GuildVoiceActivityTracker tracker = trackers.getIfPresent(channel.getGuild().getIdLong());
        if (tracker == null) {
            return;
        }
        VoiceActivityTracker.MemberState state = tracker.remove(channel, member);
        if (state != null) {
            rankingService.addVoiceActivity(member, state);
        }
        if (tracker.isEmpty()) {
            trackers.invalidate(channel.getIdLong());
        }
    }

    private void updateFreeze(Member member, GuildVoiceState state) {
        GuildVoiceActivityTracker tracker = trackers.getIfPresent(member.getGuild().getIdLong());
        if (tracker == null) {
            return;
        }
        tracker.freeze(member, isFrozen(state));
    }

    private boolean isChannelAllowed(VoiceChannel channel) {
        if (channel == null || channel.equals(channel.getGuild().getAfkChannel())) {
            return false;
        }
        RankingConfig config = rankingConfigService.get(channel.getGuild());
        return config == null
                || !config.isEnabled()
                || CollectionUtils.isEmpty(config.getIgnoredVoiceChannels())
                || !config.getIgnoredVoiceChannels().contains(channel.getIdLong());
    }

    private static boolean isFrozen(Member member) {
        return isFrozen(member.getVoiceState());
    }

    private static boolean isFrozen(GuildVoiceState voiceState) {
        return voiceState == null || !voiceState.inVoiceChannel() || voiceState.isMuted() || voiceState.isSuppressed();
    }
}