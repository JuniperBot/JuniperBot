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

import ru.juniperbot.worker.common.command.model.Command;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public interface DiscordMetricsRegistry {

    String GAUGE_GUILDS = "discord.guilds";

    String GAUGE_USERS = "discord.users";

    String GAUGE_CHANNELS = "discord.channels";

    String GAUGE_TEXT_CHANNELS = "discord.textChannels";

    String GAUGE_VOICE_CHANNELS = "discord.voiceChannels";

    String GAUGE_PING = "discord.average.ping";

    long getGuildCount();

    long getUserCount();

    long getChannelCount();

    long getTextChannelCount();

    long getVoiceChannelCount();

    double getAveragePing();

    Map<Integer, Long> getShardPings();

    void incrementCommand(Command command);

    Map<Command, AtomicLong> getCommandExecutions();
}
