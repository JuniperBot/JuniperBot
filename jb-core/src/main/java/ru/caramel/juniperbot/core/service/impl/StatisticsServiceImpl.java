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
package ru.caramel.juniperbot.core.service.impl;

import com.codahale.metrics.*;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.core.JDA;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import ru.caramel.juniperbot.core.model.ProviderStats;
import ru.caramel.juniperbot.core.persistence.entity.StoredMetric;
import ru.caramel.juniperbot.core.persistence.repository.StoredMetricRepository;
import ru.caramel.juniperbot.core.service.StatisticsService;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StatisticsServiceImpl implements StatisticsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StatisticsServiceImpl.class);

    private static final String ORG_ENDPOINT = "https://discordbots.org/api/bots/{clientId}/stats";

    private static final String PW_ENDPOINT = "https://bots.discord.pw/api/bots/{clientId}/stats";

    private RestTemplate restTemplate = new RestTemplate();

    @Value("${discord.oauth.clientId}")
    private String clientId;

    @Value("${discord.stats.discordbotsOrgToken:}")
    private String orgToken;

    @Value("${discord.stats.botsDiscordPwToken:}")
    private String pwToken;

    @Getter
    @Setter
    @Value("${discord.stats.detailedMetrics:true}")
    private boolean detailed;

    @Autowired
    private MetricRegistry metricRegistry;

    @Autowired
    private StoredMetricRepository metricRepository;

    @Override
    public Timer getTimer(String name) {
        return metricRegistry.timer(name);
    }

    @Override
    public Meter getMeter(String name) {
        return metricRegistry.meter(name);
    }

    @Override
    public Counter getCounter(String name) {
        return metricRegistry.counter(name);
    }

    @PostConstruct
    public void init() {
        try {
            loadMetrics();
        } catch (Exception e) {
            LOGGER.warn("Could not load metrics from database", e);
        }
    }

    @Override
    @Async
    public void notifyProviders(JDA shard) {
        ProviderStats stats = new ProviderStats(shard);
        notifyProvider(stats, ORG_ENDPOINT, orgToken);
        notifyProvider(stats, PW_ENDPOINT, pwToken);
    }

    private void notifyProvider(ProviderStats stats, String endPoint, String token) {
        if (StringUtils.isEmpty(token)) {
            return;
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.add("Authorization", token);
            HttpEntity<ProviderStats> request = new HttpEntity<>(stats, headers);
            ResponseEntity<String> response = restTemplate.exchange(endPoint, HttpMethod.POST, request, String.class, clientId);
            if (!HttpStatus.OK.equals(response.getStatusCode())) {
                LOGGER.warn("Could not report stats {} to endpoint {}: response is {}", stats, endPoint,
                        response.getStatusCode());
            }
        } catch (Exception e) {
            LOGGER.warn("Could not report stats {} to endpoint {}: {}", stats, endPoint, e.getMessage());
        }
    }

    private void loadMetrics() {
        List<StoredMetric> metrics = metricRepository.findAll();
        for (StoredMetric metric : metrics) {
            if (Counter.class.isAssignableFrom(metric.getType())) {
                Counter counter = getCounter(metric.getName());
                counter.inc(metric.getCount());
            }
        }
    }

    @Transactional
    @Scheduled(fixedDelay = 300000)
    public void persistMetrics() {
        synchronized (this) {
            Map<String, Metric> metricMap = metricRegistry.getMetrics();
            if (MapUtils.isNotEmpty(metricMap)) {
                metricMap = metricMap.entrySet().stream()
                        .filter(e -> e.getKey().endsWith(".persist") && e.getValue() instanceof Counter)
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            }
            if (MapUtils.isEmpty(metricMap)) {
                return;
            }

            metricMap.forEach((k, v) -> {
                Counter counter = (Counter) v;
                StoredMetric storedMetric = getOrNewMetric(k, v);
                storedMetric.setCount(counter.getCount());
                metricRepository.save(storedMetric);
            });
        }
    }

    @Override
    public void doWithTimer(String name, Runnable action) {
        doWithTimer(getTimer(name), action);
    }

    @Override
    public void doWithTimer(Timer timer, Runnable action) {
        final Timer.Context context = timer.time();
        try {
            action.run();
        } finally {
            context.stop();
        }
    }

    private StoredMetric getOrNewMetric(String name, Metric metric) {
        StoredMetric storedMetric = metricRepository.findByNameAndType(name, metric.getClass());
        if (storedMetric == null) {
            storedMetric = new StoredMetric();
            storedMetric.setName(name);
            storedMetric.setType(metric.getClass());
        }
        return storedMetric;
    }
}
