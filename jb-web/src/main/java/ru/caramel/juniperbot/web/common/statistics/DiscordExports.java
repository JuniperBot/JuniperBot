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
package ru.caramel.juniperbot.web.common.statistics;

import io.prometheus.client.dropwizard.samplebuilder.DefaultSampleBuilder;
import io.prometheus.client.dropwizard.samplebuilder.SampleBuilder;
import lavalink.client.io.RemoteStats;
import lombok.RequiredArgsConstructor;
import ru.caramel.juniperbot.core.metrics.service.DiscordMetricsRegistry;
import ru.caramel.juniperbot.module.audio.service.LavaAudioService;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class DiscordExports extends io.prometheus.client.Collector implements io.prometheus.client.Collector.Describable {

    private final static String PING_METRIC_NAME = "discord_ping";

    private final static String COMMANDS_METRIC_NAME = "commands_executed_total";

    private final static String LAVALINK_METRIC_NAME = "lavalink_players";

    private final SampleBuilder sampleBuilder = new DefaultSampleBuilder();

    private final DiscordMetricsRegistry registry;

    private final LavaAudioService lavaAudioService;

    @Override
    public List<MetricFamilySamples> collect() {
        Map<String, MetricFamilySamples> mfSamplesMap = new HashMap<String, MetricFamilySamples>();
        addToMap(mfSamplesMap, getPingSamples());
        addToMap(mfSamplesMap, getCommandSamples());
        addToMap(mfSamplesMap, getLavaLinkUpSamples());
        addToMap(mfSamplesMap, getLavaLinkStatsSamples("_total", stats -> (double) stats.getPlayers()));
        addToMap(mfSamplesMap, getLavaLinkStatsSamples("_playing_players", stats -> (double) stats.getPlayingPlayers()));
        addToMap(mfSamplesMap, getLavaLinkStatsSamples("_system_load", stats -> stats.getSystemLoad()));
        return new ArrayList<>(mfSamplesMap.values());
    }

    private MetricFamilySamples getPingSamples() {
        List<MetricFamilySamples.Sample> samples = registry.getShardPings().entrySet().stream().map(e ->
                sampleBuilder.createSample(PING_METRIC_NAME, "",
                        Collections.singletonList("shard"),
                        Collections.singletonList(String.valueOf(e.getKey() + 1)), e.getValue().doubleValue()))
                .collect(Collectors.toList());
        samples.add(sampleBuilder.createSample(PING_METRIC_NAME, "_average",
                Collections.emptyList(),
                Collections.emptyList(), registry.getAveragePing()));
        return new MetricFamilySamples(PING_METRIC_NAME, Type.SUMMARY, getHelpMessage(PING_METRIC_NAME), samples);
    }

    private MetricFamilySamples getCommandSamples() {
        List<MetricFamilySamples.Sample> samples = registry.getCommandExecutions().entrySet()
                .stream()
                .filter(e -> !e.getKey().isHidden())
                .map(e -> sampleBuilder.createSample(COMMANDS_METRIC_NAME, "",
                        Collections.singletonList("class"),
                        Collections.singletonList(e.getKey().getClass().getSimpleName()), e.getValue().doubleValue()))
                .collect(Collectors.toList());
        return new MetricFamilySamples(COMMANDS_METRIC_NAME, Type.SUMMARY, getHelpMessage(COMMANDS_METRIC_NAME), samples);
    }

    private MetricFamilySamples getLavaLinkStatsSamples(String suffix, Function<RemoteStats, Double> supplier) {
        List<MetricFamilySamples.Sample> samples = new ArrayList<>(lavaAudioService
                .getLavaLink().getNodes().size() * 2);

        String metricsName = LAVALINK_METRIC_NAME + suffix;
        lavaAudioService.getLavaLink().getNodes().forEach(node -> {
            if (node.getStats() != null) {
                sampleBuilder.createSample(metricsName, "",
                        Collections.singletonList("nodeName"),
                        Collections.singletonList(node.getName()), supplier.apply(node.getStats()));
            }
        });
        return new MetricFamilySamples(metricsName, Type.SUMMARY, getHelpMessage(metricsName), samples);
    }

    private MetricFamilySamples getLavaLinkUpSamples() {
        List<MetricFamilySamples.Sample> samples = new ArrayList<>(lavaAudioService.getLavaLink().getNodes().size());
        String metricsName = LAVALINK_METRIC_NAME + "_up";
        lavaAudioService.getLavaLink().getNodes().forEach(node -> {
            sampleBuilder.createSample(metricsName, "",
                    Collections.singletonList("nodeName"),
                    Collections.singletonList(node.getName()), node.isAvailable() ? 1 : 0);
        });
        return new MetricFamilySamples(metricsName, Type.SUMMARY, getHelpMessage(metricsName), samples);
    }

    private void addToMap(Map<String, MetricFamilySamples> mfSamplesMap, MetricFamilySamples newMfSamples) {
        if (newMfSamples != null) {
            MetricFamilySamples currentMfSamples = mfSamplesMap.get(newMfSamples.name);
            if (currentMfSamples == null) {
                mfSamplesMap.put(newMfSamples.name, newMfSamples);
            } else {
                List<MetricFamilySamples.Sample> samples = new ArrayList<MetricFamilySamples.Sample>(currentMfSamples.samples);
                samples.addAll(newMfSamples.samples);
                mfSamplesMap.put(newMfSamples.name, new MetricFamilySamples(newMfSamples.name, currentMfSamples.type, currentMfSamples.help, samples));
            }
        }
    }

    private static String getHelpMessage(String metricName) {
        return String.format("Generated from Discord metric import (metric=%s)", metricName);
    }

    @Override
    public List<MetricFamilySamples> describe() {
        return new ArrayList<>();
    }
}
