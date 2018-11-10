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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.Webhook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.caramel.juniperbot.core.persistence.entity.WebHook;
import ru.caramel.juniperbot.core.service.DiscordService;
import ru.caramel.juniperbot.core.service.WebHookService;
import ru.caramel.juniperbot.core.utils.CommonUtils;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Service
public class WebHookServiceImpl implements WebHookService {

    @Autowired
    private DiscordService discordService;

    private LoadingCache<Guild, List<Webhook>> webHooks = CacheBuilder.newBuilder()
            .concurrencyLevel(4)
            .weakKeys()
            .maximumSize(100)
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .build(
                    new CacheLoader<Guild, List<Webhook>>() {
                        public List<Webhook> load(Guild guild) {
                            return guild.getWebhooks().complete();
                        }
                    });

    public boolean updateWebHook(long guildId, Long channelId, WebHook webHook, String name, String iconUrl) {
        if (discordService.isConnected(guildId)) {
            Guild guild = discordService.getShardManager().getGuildById(guildId);
            if (guild != null && channelId != null) {
                Webhook webhook = getWebHook(guild, webHook);
                if (webhook == null) {
                    TextChannel channel = guild.getTextChannelById(channelId);
                    if (channel != null && guild.getSelfMember().hasPermission(channel, Permission.MANAGE_WEBHOOKS)) {
                        webhook = channel.createWebhook(CommonUtils.trimTo(name, 2,32))
                                .setAvatar(CommonUtils.getIcon(iconUrl))
                                .complete();
                        webHooks.invalidate(guild);
                    }
                }
                if (webhook != null) {
                    if (!channelId.equals(webhook.getChannel().getIdLong())) {
                        TextChannel channel = guild.getTextChannelById(channelId);
                        if (channel == null) {
                            throw new IllegalStateException("Tried to set non-existent channel");
                        }
                        if (guild.getSelfMember().hasPermission(channel, Permission.MANAGE_WEBHOOKS)) {
                            webhook.getManager().setChannel(channel).complete();
                        }
                    }
                    webHook.setHookId(webhook.getIdLong());
                    webHook.setToken(webhook.getToken());
                    return true;
                }
            }
        }
        return false;
    }

    public boolean delete(long guildId, WebHook webHook) {
        if (discordService.isConnected(guildId)) {
            Guild guild = discordService.getShardManager().getGuildById(guildId);
            if (guild != null && guild.getSelfMember().hasPermission(Permission.MANAGE_WEBHOOKS)) {
                Webhook webhook = getWebHook(guild, webHook);
                if (webhook != null) {
                    webhook.delete().queue();
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void invalidateCache(long guildId) {
        Guild guild = webHooks.asMap().keySet().stream().filter(e -> e.getIdLong() == guildId).findFirst().orElse(null);
        if (guild != null) {
            webHooks.invalidate(guild);
        }
    }

    @Override
    public Webhook getWebHook(Guild guild, WebHook webHook) {
        if (webHook.getHookId() != null && webHook.getToken() != null
                && guild.getSelfMember().hasPermission(Permission.MANAGE_WEBHOOKS)) {
            try {
                return webHooks.get(guild).stream()
                        .filter(e -> webHook.getHookId().equals(e.getIdLong())
                                && webHook.getToken().equals(e.getToken())).findFirst().orElse(null);
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }
}
