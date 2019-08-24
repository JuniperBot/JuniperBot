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
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import ru.juniperbot.common.persistence.entity.RankingConfig;
import ru.juniperbot.common.service.RankingConfigService;
import ru.juniperbot.common.worker.event.DiscordEvent;
import ru.juniperbot.common.worker.event.listeners.DiscordEventListener;
import ru.juniperbot.module.ranking.service.RankingService;

import java.util.concurrent.TimeUnit;

@DiscordEvent(priority = 0)
public class VoiceListener extends DiscordEventListener {

    @Autowired
    private RankingConfigService rankingConfigService;

    @Autowired
    private RankingService rankingService;

    private Cache<String, Long> activities = CacheBuilder.newBuilder()
            .concurrencyLevel(4)
            .expireAfterWrite(10, TimeUnit.DAYS)
            .build();

    @Override
    public void onGuildVoiceJoin(GuildVoiceJoinEvent event) {
        if (event.getMember().getUser().isBot() || isIgnoredChannel(event.getChannelJoined())) {
            return;
        }
        startRecord(event.getMember());
    }

    @Override
    public void onGuildVoiceMove(GuildVoiceMoveEvent event) {
        if (event.getMember().getUser().isBot()) {
            return;
        }
        if (isIgnoredChannel(event.getChannelJoined())) {
            stopRecord(event.getMember());
        } else {
            startRecord(event.getMember());
        }
    }

    @Override
    public void onGuildVoiceLeave(GuildVoiceLeaveEvent event) {
        if (event.getMember().getUser().isBot() || isIgnoredChannel(event.getChannelLeft())) {
            return;
        }
        stopRecord(event.getMember());
    }

    private void startRecord(Member member) {
        String key = getMemberKey(member);
        if (activities.getIfPresent(key) == null) {
            activities.put(key, System.currentTimeMillis());
        }
    }

    private void stopRecord(Member member) {
        String key = getMemberKey(member);
        Long startTime = activities.getIfPresent(key);
        if (startTime == null) {
            return;
        }
        activities.invalidate(key);
        rankingService.addVoiceActivity(member, System.currentTimeMillis() - startTime);
    }

    private boolean isIgnoredChannel(VoiceChannel channel) {
        if (channel == null || channel.equals(channel.getGuild().getAfkChannel())) {
            return true;
        }
        RankingConfig config = rankingConfigService.get(channel.getGuild());
        if (config == null) {
            return false;
        }
        return CollectionUtils.isNotEmpty(config.getIgnoredVoiceChannels())
                && config.getIgnoredVoiceChannels().contains(channel.getIdLong());
    }

    private static String getMemberKey(Member member) {
        return String.format("%s/%s", member.getGuild().getId(), member.getUser().getId());
    }
}