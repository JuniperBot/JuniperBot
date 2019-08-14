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
package ru.caramel.juniperbot.web.dao;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import lavalink.client.io.LavalinkSocket;
import lavalink.client.io.RemoteStats;
import net.dv8tion.jda.api.JDA;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.caramel.juniperbot.core.common.service.DiscordService;
import ru.caramel.juniperbot.core.metrics.service.DiscordMetricsRegistry;
import ru.caramel.juniperbot.module.audio.service.LavaAudioService;
import ru.caramel.juniperbot.module.audio.service.PlayerService;
import ru.caramel.juniperbot.web.dto.LavaLinkNodeDto;
import ru.caramel.juniperbot.web.dto.ShardDto;
import ru.caramel.juniperbot.web.dto.StatusDto;

import java.util.Comparator;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class StatusDao extends AbstractDao {

    @Autowired
    private MetricRegistry metricRegistry;

    @Autowired
    private DiscordService discordService;

    @Autowired
    private LavaAudioService lavaAudioService;

    @Transactional(readOnly = true)
    public StatusDto get() {
        Map<String, Metric> metricMap = metricRegistry.getMetrics();

        StatusDto result = new StatusDto();
        result.setGuildCount(getMetricGauge(metricMap, DiscordMetricsRegistry.GAUGE_GUILDS));
        result.setUserCount(getMetricGauge(metricMap, DiscordMetricsRegistry.GAUGE_USERS));
        result.setTextChannelCount(getMetricGauge(metricMap, DiscordMetricsRegistry.GAUGE_TEXT_CHANNELS));
        result.setVoiceChannelCount(getMetricGauge(metricMap, DiscordMetricsRegistry.GAUGE_VOICE_CHANNELS));
        result.setActiveConnections(getMetricGauge(metricMap, PlayerService.ACTIVE_CONNECTIONS));
        result.setExecutedCommands(getMetricGauge(metricMap, "commands.executions.persist"));
        result.setUptimeDuration(getMetricGauge(metricMap, "jvm.uptime"));

        result.setShards(discordService.getShardManager().getShards().stream()
                .sorted(Comparator.comparing(e -> e.getShardInfo().getShardId()))
                .map(e -> {
                    ShardDto dto = new ShardDto();
                    dto.setId(e.getShardInfo().getShardId());
                    dto.setGuilds(e.getGuildCache().size());
                    dto.setUsers(e.getUserCache().size());
                    dto.setChannels(e.getTextChannelCache().size() + e.getVoiceChannelCache().size());
                    dto.setPing(e.getGatewayPing());
                    dto.setConnected(JDA.Status.CONNECTED.equals(e.getStatus()));
                    return dto;
                })
                .collect(Collectors.toList()));

        if (lavaAudioService.getConfiguration().isEnabled()) {
            result.setLinkNodes(lavaAudioService.getLavaLink().getNodes().stream()
                    .sorted(Comparator.comparing(LavalinkSocket::getName))
                    .map(e -> {
                        LavaLinkNodeDto nodeDto = new LavaLinkNodeDto();
                        nodeDto.setName(e.getName());
                        nodeDto.setAvailable(e.isAvailable());
                        RemoteStats stats = e.getStats();
                        if (stats != null) {
                            nodeDto.setPlayers(stats.getPlayers());
                            nodeDto.setPlayingPlayers(stats.getPlayingPlayers());
                            nodeDto.setLavalinkLoad(stats.getLavalinkLoad());
                            nodeDto.setSystemLoad(stats.getSystemLoad());
                        }
                        return nodeDto;
                    }).collect(Collectors.toList()));
        }
        return result;
    }

    private static Long getMetricGauge(Map<String, Metric> metricMap, String name) {
        return getMetricValue(metricMap, name, Long.class, 0L);
    }

    @SuppressWarnings("unchecked")
    private static <T> T getMetricValue(Map<String, Metric> metricMap, String name, Class<T> type, T defaultValue) {
        Object result = getMetricValue(metricMap, name, null);
        if (result != null && type.isAssignableFrom(result.getClass())) {
            return (T) result;
        }
        return defaultValue;
    }

    @SuppressWarnings("unchecked")
    private static <T> T getMetricValue(Map<String, Metric> metricMap, String name, Function<Object, T> valueExtractor) {
        Metric metric = metricMap.get(name);
        T value = null;
        if (metric instanceof Gauge) {
            Gauge gauge = (Gauge) metric;
            value = (T) gauge.getValue();
        }
        if (metric instanceof Counter) {
            Counter counter = (Counter) metric;
            value = (T) (Long) counter.getCount();
        }
        if (value != null && valueExtractor != null) {
            value = valueExtractor.apply(value);
        }
        return value;
    }
}
