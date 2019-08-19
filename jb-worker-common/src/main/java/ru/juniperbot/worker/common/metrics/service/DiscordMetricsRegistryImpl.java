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
package ru.juniperbot.worker.common.metrics.service;

import com.codahale.metrics.annotation.CachedGauge;
import com.codahale.metrics.annotation.Gauge;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.juniperbot.worker.common.command.model.Command;
import ru.juniperbot.worker.common.shared.service.DiscordService;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Component
public class DiscordMetricsRegistryImpl implements DiscordMetricsRegistry {

    private final Map<Command, AtomicLong> commandExecutions = new ConcurrentHashMap<>();

    @Autowired
    private DiscordService discordService;

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
        return getShardManager() != null ? getShardManager().getAverageGatewayPing() : 0;
    }

    @Override
    public Map<Integer, Long> getShardPings() {
        return getShardManager().getShards().stream()
                .collect(Collectors.toMap(e -> e.getShardInfo().getShardId(),
                        e -> JDA.Status.CONNECTED.equals(e.getStatus()) ? e.getGatewayPing() : -1));
    }

    @Override
    public void incrementCommand(Command command) {
        commandExecutions.computeIfAbsent(command, e -> new AtomicLong(0)).incrementAndGet();
    }

    @Override
    public Map<Command, AtomicLong> getCommandExecutions() {
        return Collections.unmodifiableMap(commandExecutions);
    }

    private ShardManager getShardManager() {
        return discordService.getShardManager();
    }
}
