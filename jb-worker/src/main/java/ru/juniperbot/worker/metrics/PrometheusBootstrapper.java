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
package ru.juniperbot.worker.metrics;

import com.codahale.metrics.MetricRegistry;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.dropwizard.DropwizardExports;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.caramel.juniperbot.module.audio.service.LavaAudioService;
import ru.juniperbot.common.worker.metrics.service.DiscordMetricsRegistry;

import javax.annotation.PostConstruct;

@Component
public class PrometheusBootstrapper {

    @Autowired
    private MetricRegistry metricRegistry;

    @Autowired
    private DiscordMetricsRegistry discordMetricsRegistry;

    @Autowired
    private LavaAudioService lavaAudioService;

    @PostConstruct
    public void init() {
        CollectorRegistry.defaultRegistry.clear();
        new DropwizardExports(metricRegistry).register();
        new DiscordExports(discordMetricsRegistry, lavaAudioService).register();
    }
}
