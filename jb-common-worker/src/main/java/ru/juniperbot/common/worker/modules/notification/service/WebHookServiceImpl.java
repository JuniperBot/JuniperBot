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
package ru.juniperbot.common.worker.modules.notification.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.Webhook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.juniperbot.common.persistence.entity.WebHook;
import ru.juniperbot.common.persistence.repository.WebHookRepository;
import ru.juniperbot.common.utils.CommonUtils;
import ru.juniperbot.common.worker.shared.service.DiscordService;
import ru.juniperbot.common.worker.utils.DiscordUtils;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class WebHookServiceImpl implements WebHookService {

    @Autowired
    private DiscordService discordService;

    @Autowired
    private WebHookRepository repository;

    private LoadingCache<Guild, List<Webhook>> webHooks = CacheBuilder.newBuilder()
            .concurrencyLevel(4)
            .weakKeys()
            .maximumSize(100)
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .build(
                    new CacheLoader<Guild, List<Webhook>>() {
                        public List<Webhook> load(Guild guild) {
                            return guild.retrieveWebhooks().complete();
                        }
                    });

    @Override
    public boolean updateWebHook(long id, long guildId, String channelId, String name, String iconUrl) {
        if (discordService.isConnected(guildId)) {
            Guild guild = discordService.getShardManager().getGuildById(guildId);
            if (guild != null && channelId != null) {
                Webhook webhook = getWebHook(id, guild);
                if (webhook != null) {
                    checkWebhookChannel(webhook, channelId);
                    return true;
                }
                TextChannel channel = guild.getTextChannelById(channelId);
                if (channel != null && guild.getSelfMember().hasPermission(channel, Permission.MANAGE_WEBHOOKS)) {
                    channel.createWebhook(CommonUtils.trimTo(name, 2, 32))
                            .setAvatar(DiscordUtils.createIcon(iconUrl))
                            .queue(e -> {
                                checkWebhookChannel(e, channelId);
                                WebHook webHook = repository.findById(id).orElse(null);
                                if (webHook != null) {
                                    webHook.setHookId(e.getIdLong());
                                    webHook.setToken(e.getToken());
                                    repository.save(webHook);
                                }
                            });
                    webHooks.invalidate(guild);
                }
                return true;
            }
        }
        return false;
    }

    private void checkWebhookChannel(Webhook webhook, String channelId) {
        Guild guild = webhook.getGuild();
        if (!channelId.equals(webhook.getChannel().getId())) {
            TextChannel channel = guild.getTextChannelById(channelId);
            if (channel == null) {
                throw new IllegalStateException("Tried to set non-existent channel");
            }
            if (guild.getSelfMember().hasPermission(channel, Permission.MANAGE_WEBHOOKS)) {
                webhook.getManager().setChannel(channel).queue();
            }
        }
    }

    @Override
    public boolean delete(long id, long guildId) {
        if (discordService.isConnected(guildId)) {
            Guild guild = discordService.getShardManager().getGuildById(guildId);
            if (guild != null && guild.getSelfMember().hasPermission(Permission.MANAGE_WEBHOOKS)) {
                Webhook webhook = getWebHook(id, guild);
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
        webHooks.asMap().keySet()
                .stream()
                .filter(e -> e.getIdLong() == guildId)
                .findFirst()
                .ifPresent(webHooks::invalidate);
    }

    @Override
    public Webhook getWebHook(long id, Guild guild) {
        if (!guild.getSelfMember().hasPermission(Permission.MANAGE_WEBHOOKS)) {
            return null;
        }
        WebHook webHook = repository.findById(id).orElse(null);
        if (webHook == null
                || webHook.getHookId() == null
                || webHook.getToken() == null) {
            return null;
        }
        try {
            return webHooks.get(guild).stream()
                    .filter(e -> webHook.getHookId().equals(e.getIdLong()) && webHook.getToken().equals(e.getToken()))
                    .findFirst()
                    .orElse(null);
        } catch (ExecutionException e) {
            log.error("Could not get webhook {}", id, e);
        }
        return null;
    }
}
