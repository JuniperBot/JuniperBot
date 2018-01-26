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
package ru.caramel.juniperbot.core.service;

import net.dv8tion.jda.bot.sharding.ShardManager;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.webhook.WebhookMessage;
import ru.caramel.juniperbot.core.persistence.entity.WebHook;

import java.util.function.Consumer;

public interface DiscordService {

    String GAUGE_GUILDS = "discord.guilds";

    String GAUGE_USERS = "discord.users";

    String GAUGE_CHANNELS = "discord.channels";

    String GAUGE_PING = "discord.ping";

    JDA getJda();

    User getSelfUser();

    JDA getShard(long guildId);

    ShardManager getShardManager();

    boolean isConnected();

    boolean isConnected(long guildId);

    boolean isSuperUser(User user);

    VoiceChannel getDefaultMusicChannel(long guildId);

    void executeWebHook(WebHook webHook, WebhookMessage message, Consumer<WebHook> onAbsent);

    long getGuildCount();

    long getUserCount();

    long getChannelCount();

    double getAveragePing();
}
