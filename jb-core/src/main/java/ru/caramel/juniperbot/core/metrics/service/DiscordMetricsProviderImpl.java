/*
 * This file is part of JuniperBotJ.
 *
 * JuniperBotJ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * JuniperBotJ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with JuniperBotJ. If not, see <http://www.gnu.org/licenses/>.
 */
package ru.caramel.juniperbot.core.metrics.service;

import com.codahale.metrics.annotation.CachedGauge;
import com.codahale.metrics.annotation.Gauge;
import net.dv8tion.jda.bot.sharding.ShardManager;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.caramel.juniperbot.core.common.service.DiscordService;
import ru.caramel.juniperbot.core.metrics.model.TimeWindowChart;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class DiscordMetricsProviderImpl extends ListenerAdapter implements DiscordMetricsProvider {

    @Autowired
    private DiscordService discordService;

    @Autowired
    private StatisticsService statisticsService;

    @Deprecated // remove on Prometheus alternative found
    private Map<JDA, TimeWindowChart> pingCharts = new HashMap<>();

    @Scheduled(fixedDelay = 30000)
    public void tickPing() {
        if (MapUtils.isEmpty(pingCharts)) {
            return;
        }
        getShardManager().getShards().forEach(e -> {
            TimeWindowChart reservoir = pingCharts.get(e);
            if (reservoir != null) {
                reservoir.update(JDA.Status.CONNECTED.equals(e.getStatus()) ? e.getPing() : -1);
            }
        });
    }

    @Override
    public void onReady(ReadyEvent event) {
        TimeWindowChart chart = statisticsService
                .getTimeChart(String.format("jda.shard.ping.%s.persist", event.getJDA().getShardInfo().getShardId()),
                        10, TimeUnit.MINUTES);
        pingCharts.put(event.getJDA(), chart);
    }

    @Override
    public Map<JDA, TimeWindowChart> getPingCharts() {
        return Collections.unmodifiableMap(pingCharts);
    }

    @Override
    @CachedGauge(name = GAUGE_GUILDS, absolute = true, timeout = 15, timeoutUnit = TimeUnit.SECONDS)
    public long getGuildCount() {
        return getShardManager() != null ? getShardManager().getGuildCache().size() : 0;
    }

    @Override
    @CachedGauge(name = GAUGE_USERS, absolute = true, timeout = 15, timeoutUnit = TimeUnit.SECONDS)
    public long getUserCount() {
        return getShardManager() != null ? getShardManager().getUserCache().size() : 0;
    }

    @Override
    @CachedGauge(name = GAUGE_CHANNELS, absolute = true, timeout = 15, timeoutUnit = TimeUnit.SECONDS)
    public long getChannelCount() {
        return getTextChannelCount() + getVoiceChannelCount();
    }

    @Override
    @CachedGauge(name = GAUGE_TEXT_CHANNELS, absolute = true, timeout = 15, timeoutUnit = TimeUnit.SECONDS)
    public long getTextChannelCount() {
        return getShardManager() != null ? getShardManager().getTextChannelCache().size() : 0;
    }

    @Override
    @CachedGauge(name = GAUGE_VOICE_CHANNELS, absolute = true, timeout = 15, timeoutUnit = TimeUnit.SECONDS)
    public long getVoiceChannelCount() {
        return getShardManager() != null ? getShardManager().getVoiceChannelCache().size() : 0;
    }

    @Override
    @Gauge(name = GAUGE_PING, absolute = true)
    public double getAveragePing() {
        return getShardManager() != null ? getShardManager().getAveragePing() : 0;
    }

    private ShardManager getShardManager() {
        return discordService.getShardManager();
    }
}
