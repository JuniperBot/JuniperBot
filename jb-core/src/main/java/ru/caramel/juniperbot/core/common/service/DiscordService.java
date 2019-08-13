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
package ru.caramel.juniperbot.core.common.service;

import club.minnced.discord.webhook.send.WebhookMessage;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.sharding.ShardManager;
import ru.caramel.juniperbot.core.subscription.persistence.WebHook;

import java.util.function.Consumer;

public interface DiscordService {

    String getUserId();

    int getShardsNum();

    JDA getJda();

    User getSelfUser();

    JDA getShardById(int guildId);

    Guild getGuildById(long guildId);

    User getUserById(long userId);

    TextChannel getTextChannelById(long channelId);

    TextChannel getTextChannelById(String channelId);

    VoiceChannel getVoiceChannelById(long channelId);

    VoiceChannel getVoiceChannelById(String channelId);

    JDA getShard(long guildId);

    ShardManager getShardManager();

    boolean isConnected();

    boolean isConnected(long guildId);

    boolean isSuperUser(User user);

    VoiceChannel getDefaultMusicChannel(long guildId);

    Member getMember(long guildId, long userId);

    void executeWebHook(WebHook webHook, WebhookMessage message, Consumer<WebHook> onAbsent);
}
