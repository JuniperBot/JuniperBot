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
package ru.caramel.juniperbot.web.dao.api;

import com.codahale.metrics.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.caramel.juniperbot.core.service.DiscordService;
import ru.caramel.juniperbot.module.audio.service.PlayerService;
import ru.caramel.juniperbot.web.dao.AbstractDao;
import ru.caramel.juniperbot.web.dto.api.StatusDto;

import java.util.Map;
import java.util.function.Function;

@Service
public class StatusDao extends AbstractDao {

    @Autowired
    private MetricRegistry metricRegistry;

    @Transactional(readOnly = true)
    public StatusDto get() {
        Map<String, Metric> metricMap = metricRegistry.getMetrics();

        StatusDto result = new StatusDto();
        result.setGuildCount(getMetricGauge(metricMap, DiscordService.GAUGE_GUILDS));
        result.setUserCount(getMetricGauge(metricMap, DiscordService.GAUGE_USERS));
        result.setTextChannelCount(getMetricGauge(metricMap, DiscordService.GAUGE_TEXT_CHANNELS));
        result.setVoiceChannelCount(getMetricGauge(metricMap, DiscordService.GAUGE_VOICE_CHANNELS));
        result.setActiveConnections(getMetricGauge(metricMap, PlayerService.ACTIVE_CONNECTIONS));
        result.setExecutedCommands(getMetricGauge(metricMap, "commands.executions.persist"));
        result.setUptimeDuration(getMetricGauge(metricMap, "jvm.uptime"));

        Meter commandRate = (Meter) metricMap.get("commands.executions.rate");
        if (commandRate != null) {
            result.setCommandsRateMean(commandRate.getMeanRate());
            result.setCommandsRate1m(commandRate.getOneMinuteRate());
            result.setCommandsRate5m(commandRate.getFiveMinuteRate());
            result.setCommandsRate15m(commandRate.getFifteenMinuteRate());
        }
        return result;
    }

    private static Long getMetricGauge(Map<String,Metric> metricMap, String name) {
        return getMetricValue(metricMap, name, Long.class, 0L);
    }

    @SuppressWarnings("unchecked")
    private static <T> T getMetricValue(Map<String,Metric> metricMap, String name, Class<T> type, T defaultValue) {
        Object result = getMetricValue(metricMap, name, null);
        if (result != null && type.isAssignableFrom(result.getClass())) {
            return (T) result;
        }
        return defaultValue;
    }

    @SuppressWarnings("unchecked")
    private static <T> T getMetricValue(Map<String,Metric> metricMap, String name, Function<Object, T> valueExtractor) {
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
