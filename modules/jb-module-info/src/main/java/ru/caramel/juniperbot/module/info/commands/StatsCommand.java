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
package ru.caramel.juniperbot.module.info.commands;

import com.codahale.metrics.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.ocpsoft.prettytime.PrettyTime;
import org.springframework.beans.factory.annotation.Autowired;
import ru.juniperbot.worker.common.command.model.BotContext;
import ru.juniperbot.worker.common.command.model.DiscordCommand;
import ru.juniperbot.worker.common.metrics.service.DiscordMetricsRegistry;

import java.util.Date;
import java.util.Map;
import java.util.function.Function;

@DiscordCommand(key = "discord.command.stats.key",
        description = "discord.command.stats.desc",
        group = "discord.command.group.info",
        priority = 1)
public class StatsCommand extends AbstractInfoCommand {

    @Autowired
    private MetricRegistry metricRegistry;

    @Override
    public boolean doCommand(GuildMessageReceivedEvent message, BotContext context, String content) {
        EmbedBuilder builder = messageService.getBaseEmbed(true);
        builder.setTitle(messageService.getMessage("discord.command.stats.title"));
        builder.setThumbnail(brandingService.getSmallAvatarUrl());

        Map<String, Metric> metricMap = metricRegistry.getMetrics();
        builder.addField(getCommonStats(metricMap));
        builder.addField(getCommandStats(metricMap));
        builder.addField(getPlatformStats(metricMap));

        messageService.sendMessageSilent(message.getChannel()::sendMessage, builder.build());
        return true;
    }

    private MessageEmbed.Field getCommonStats(Map<String, Metric> metricMap) {
        String value =
                getGaugeValue(metricMap, DiscordMetricsRegistry.GAUGE_GUILDS) + "\n" +
                        getGaugeValue(metricMap, DiscordMetricsRegistry.GAUGE_USERS) + "\n" +
                        getGaugeValue(metricMap, DiscordMetricsRegistry.GAUGE_TEXT_CHANNELS) + "\n" +
                        getGaugeValue(metricMap, DiscordMetricsRegistry.GAUGE_PING) + "\n" +
                        getGaugeValue(metricMap, "player.activeConnections") + "\n";
        return new MessageEmbed.Field(messageService.getMessage("discord.command.stats.common"), value, true);
    }

    private MessageEmbed.Field getCommandStats(Map<String, Metric> metricMap) {
        String value = getCounterValue(metricMap, "commands.executions.persist") + "\n" +
                getMeterValue(metricMap, "commands.executions.rate");
        return new MessageEmbed.Field(messageService.getMessage("discord.command.stats.commands"), value, true);
    }

    private MessageEmbed.Field getPlatformStats(Map<String, Metric> metricMap) {
        String value = getGaugeValue(metricMap, "jvm.uptime", e -> {
            Date date = new Date();
            date.setTime(date.getTime() - (long) e);
            return new PrettyTime(contextService.getLocale()).format(date);
        });
        return value != null ? new MessageEmbed.Field(messageService.getMessage("discord.command.stats.platform"), value, false) : null;
    }

    private String getCounterValue(Map<String, Metric> metricMap, String name) {
        Metric metric = metricMap.get(name);
        if (metric instanceof Counter) {
            Counter counter = (Counter) metric;
            return messageService.getMessage("discord.command.stats.values." + name, counter.getCount());
        }
        return null;
    }

    private String getGaugeValue(Map<String, Metric> metricMap, String name) {
        return getGaugeValue(metricMap, name, null);
    }

    private String getGaugeValue(Map<String, Metric> metricMap, String name, Function<Object, String> valueExtractor) {
        Metric metric = metricMap.get(name);
        if (metric instanceof Gauge) {
            Gauge gauge = (Gauge) metric;
            Object value = gauge.getValue();
            if (value != null && valueExtractor != null) {
                value = valueExtractor.apply(value);
            }
            return messageService.getMessage("discord.command.stats.values." + name, value);
        }
        return null;
    }

    private String getMeterValue(Map<String, Metric> metricMap, String name) {
        Metric metric = metricMap.get(name);
        if (metric instanceof Meter) {
            Meter meter = (Meter) metric;
            return String.format("%s\n%s\n%s\n%s",
                    messageService.getMessage("discord.command.stats.values." + name + ".mean", meter.getMeanRate()),
                    messageService.getMessage("discord.command.stats.values." + name + ".1m", meter.getOneMinuteRate()),
                    messageService.getMessage("discord.command.stats.values." + name + ".5m", meter.getFiveMinuteRate()),
                    messageService.getMessage("discord.command.stats.values." + name + ".15m", meter.getFifteenMinuteRate()));
        }
        return null;
    }
}
