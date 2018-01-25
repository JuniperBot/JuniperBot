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

import lombok.Getter;
import net.dv8tion.jda.bot.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.bot.sharding.ShardManager;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.audio.factory.IAudioSendFactory;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.ExceptionEvent;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.hooks.IEventManager;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.webhook.WebhookClient;
import net.dv8tion.jda.webhook.WebhookClientBuilder;
import net.dv8tion.jda.webhook.WebhookMessage;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.caramel.juniperbot.core.persistence.entity.WebHook;
import ru.caramel.juniperbot.core.service.DiscordService;
import ru.caramel.juniperbot.core.service.MessageService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.security.auth.login.LoginException;
import java.util.Objects;
import java.util.function.Consumer;

@Service
public class DiscordServiceImpl extends ListenerAdapter implements DiscordService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiscordServiceImpl.class);

    @Value("${discord.engine.shards:2}")
    private Integer shards;

    @Value("${discord.engine.corePoolSize:5}")
    private Integer corePoolSize;

    @Value("${discord.client.token}")
    private String token;

    @Value("${discord.client.accountType:BOT}")
    private AccountType accountType;

    @Value("${discord.client.playingStatus:}")
    private String playingStatus;

    @Value("${discord.client.superUserId:}")
    private String superUserId;

    @Autowired
    private MessageService messageService;

    @Autowired(required = false)
    private IAudioSendFactory audioSendFactory;

    @Autowired
    private IEventManager eventManager;

    @Getter
    private ShardManager shardManager;

    @PostConstruct
    public void init() {
        Objects.requireNonNull(token, "No Discord Token specified");
        try {
            DefaultShardManagerBuilder builder = new DefaultShardManagerBuilder()
                    .setToken(token)
                    .setEventManager(eventManager)
                    .addEventListeners(this)
                    .setCorePoolSize(corePoolSize)
                    .setShardsTotal(shards)
                    .setShards(0, shards - 1)
                    .setEnableShutdownHook(false);
            if (audioSendFactory != null) {
                builder.setAudioSendFactory(audioSendFactory);
            }
            shardManager = builder.build();
        } catch (LoginException e) {
            LOGGER.error("Could not login user with specified token", e);
        }
    }

    @PreDestroy
    public void destroy() {
        shardManager.shutdown();
    }

    @Override
    public void onReady(ReadyEvent event) {
        if (StringUtils.isNotEmpty(playingStatus)) {
            shardManager.setGame(Game.playing(playingStatus));
        }
    }

    @Override
    public void onException(ExceptionEvent event) {
        LOGGER.error("JDA error", event.getCause());
    }

    @Override
    public void executeWebHook(WebHook webHook, WebhookMessage message, Consumer<WebHook> onAbsent) {
        if (message != null) {
            WebhookClient client = new WebhookClientBuilder(webHook.getHookId(), webHook.getToken()).build();
            client.send(message).exceptionally(e -> {
                LOGGER.error("Can't execute webhook: ", e);
                if (e.getMessage().contains("Request returned failure 404")) {
                    onAbsent.accept(webHook);
                }
                return null;
            });
        }
    }

    @Override
    public boolean isConnected() {
        return getJda() != null && JDA.Status.CONNECTED.equals(getJda().getStatus());
    }

    @Override
    public boolean isConnected(long guildId) {
        return JDA.Status.CONNECTED.equals(getShard(guildId).getStatus());
    }

    @Override
    public JDA getJda() {
        return shardManager.getShards().iterator().next();
    }

    @Override
    public JDA getShard(long guildId) {
        return shardManager.getShardById((int)((guildId >> 22) % shards));
    }

    @Override
    public boolean isSuperUser(User user) {
        return user != null && Objects.equals(user.getId(), superUserId);
    }

    @Override
    public VoiceChannel getDefaultMusicChannel(long guildId) {
        if (!isConnected(guildId)) {
            return null;
        }
        Guild guild = shardManager.getGuildById(guildId);
        if (guild == null) {
            return null;
        }
        VoiceChannel channel;
        String channels = messageService.getMessage("discord.command.audio.channels");
        if (StringUtils.isNotEmpty(channels)) {
            for (String name : channels.split(",")) {
                channel = guild.getVoiceChannelsByName(name, true).stream().findAny().orElse(null);
                if (channel != null) {
                    return channel;
                }
            }
        }
        return guild.getVoiceChannels().stream().findAny().orElse(null);
    }
}
