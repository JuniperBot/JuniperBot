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

import io.prometheus.client.dropwizard.samplebuilder.DefaultSampleBuilder;
import io.prometheus.client.dropwizard.samplebuilder.SampleBuilder;
import lavalink.client.io.RemoteStats;
import lombok.RequiredArgsConstructor;
import ru.caramel.juniperbot.module.audio.service.LavaAudioService;
import ru.juniperbot.common.worker.metrics.service.DiscordMetricsRegistry;

import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class DiscordExports extends io.prometheus.client.Collector implements io.prometheus.client.Collector.Describable {

    private final static String PING_METRIC_NAME = "discord_ping";

    private final static String COMMANDS_METRIC_NAME = "commands_executed_total";

    private final static String LAVALINK_METRIC_NAME = "lavalink";

    private final SampleBuilder sampleBuilder = new DefaultSampleBuilder();

    private final DiscordMetricsRegistry registry;

    private final LavaAudioService lavaAudioService;

    @Override
    public List<MetricFamilySamples> collect() {
        Map<String, MetricFamilySamples> mfSamplesMap = new HashMap<String, MetricFamilySamples>();
        addToMap(mfSamplesMap, getPingSamples());
        addToMap(mfSamplesMap, getCommandSamples());
        addToMap(mfSamplesMap, getLavaLinkSamples());
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

    private MetricFamilySamples getLavaLinkSamples() {
        List<MetricFamilySamples.Sample> samples = new ArrayList<>(lavaAudioService
                .getLavaLink().getNodes().size() * 2);

        lavaAudioService.getLavaLink().getNodes().forEach(node -> {
            RemoteStats stats = node.getStats();
            if (stats == null) {
                return;
            }
            samples.add(sampleBuilder.createSample(LAVALINK_METRIC_NAME, "_total_players",
                    Collections.singletonList("nodeName"),
                    Collections.singletonList(node.getName()), stats.getPlayers()));
            samples.add(sampleBuilder.createSample(LAVALINK_METRIC_NAME, "_playing_players",
                    Collections.singletonList("nodeName"),
                    Collections.singletonList(node.getName()), stats.getPlayingPlayers()));
            samples.add(sampleBuilder.createSample(LAVALINK_METRIC_NAME, "_system_load",
                    Collections.singletonList("nodeName"),
                    Collections.singletonList(node.getName()), stats.getSystemLoad()));
            samples.add(sampleBuilder.createSample(LAVALINK_METRIC_NAME, "_up",
                    Collections.singletonList("nodeName"),
                    Collections.singletonList(node.getName()), node.isAvailable() ? 1 : 0));
        });
        return new MetricFamilySamples(LAVALINK_METRIC_NAME, Type.SUMMARY, getHelpMessage(LAVALINK_METRIC_NAME), samples);
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
