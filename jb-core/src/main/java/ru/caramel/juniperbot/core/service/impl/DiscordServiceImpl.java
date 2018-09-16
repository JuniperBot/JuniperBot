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

import com.codahale.metrics.annotation.CachedGauge;
import com.codahale.metrics.annotation.Gauge;
import lombok.Getter;
import net.dv8tion.jda.bot.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.bot.sharding.ShardManager;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.ExceptionEvent;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.hooks.IEventManager;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.requests.Requester;
import net.dv8tion.jda.webhook.WebhookClient;
import net.dv8tion.jda.webhook.WebhookClientBuilder;
import net.dv8tion.jda.webhook.WebhookMessage;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jmx.export.MBeanExporter;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.caramel.juniperbot.core.model.TimeWindowChart;
import ru.caramel.juniperbot.core.persistence.entity.WebHook;
import ru.caramel.juniperbot.core.service.AudioService;
import ru.caramel.juniperbot.core.service.DiscordService;
import ru.caramel.juniperbot.core.service.MessageService;
import ru.caramel.juniperbot.core.support.DiscordHttpRequestFactory;
import ru.caramel.juniperbot.core.support.ModuleListener;
import ru.caramel.juniperbot.core.support.jmx.JmxJDAMBean;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.security.auth.login.LoginException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Service
public class DiscordServiceImpl extends ListenerAdapter implements DiscordService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiscordServiceImpl.class);

    @Getter
    @Value("${discord.engine.shards:2}")
    private int shardsNum;

    @Value("${discord.engine.corePoolSize:5}")
    private int corePoolSize;

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

    @Autowired
    private IEventManager eventManager;

    @Getter
    private ShardManager shardManager;

    @Autowired(required = false)
    private List<ModuleListener> moduleListeners;

    @Autowired(required = false)
    private AudioService audioService;

    @Autowired
    private MBeanExporter mBeanExporter;

    private Map<JDA, TimeWindowChart> pingCharts = new HashMap<>();

    private RestTemplate restTemplate;

    private volatile String cachedUserId;

    @PostConstruct
    public void init() {
        Objects.requireNonNull(token, "No Discord Token specified");
        restTemplate = new RestTemplate(new DiscordHttpRequestFactory(token));
        try {
            DefaultShardManagerBuilder builder = new DefaultShardManagerBuilder()
                    .setToken(token)
                    .setEventManager(eventManager)
                    .addEventListeners(this)
                    .setCorePoolSize(corePoolSize)
                    .setShardsTotal(shardsNum)
                    .setShards(0, shardsNum - 1)
                    .setEnableShutdownHook(false);
            if (audioService != null) {
                audioService.configure(this, builder);
            }
            shardManager = builder.build();
        } catch (LoginException e) {
            LOGGER.error("Could not login user with specified token", e);
        }
    }

    @PreDestroy
    public void destroy() {
        // destroy every service manually before discord shutdown
        if (CollectionUtils.isNotEmpty(moduleListeners)) {
            moduleListeners.forEach(listener -> {
                try {
                    listener.onShutdown();
                } catch (Exception e) {
                    LOGGER.error("Could not shutdown listener [{}] correctly", listener, e);
                }
            });
        }
        shardManager.shutdown();
    }

    @Override
    public void onReady(ReadyEvent event) {
        if (StringUtils.isNotEmpty(playingStatus)) {
            shardManager.setGame(Game.playing(playingStatus));
        }
        pingCharts.put(event.getJDA(), new TimeWindowChart(10, TimeUnit.MINUTES));
        mBeanExporter.registerManagedResource(new JmxJDAMBean(event.getJDA()));
    }

    @Override
    public void onException(ExceptionEvent event) {
        LOGGER.error("JDA error", event.getCause());
    }

    @Override
    public void executeWebHook(WebHook webHook, WebhookMessage message, Consumer<WebHook> onAbsent) {
        if (message != null) {
            try (WebhookClient client = new WebhookClientBuilder(webHook.getHookId(), webHook.getToken()).build()) {
                client.send(message).exceptionally(e -> {
                    LOGGER.error("Can't execute webhook: ", e);
                    if (e.getMessage().contains("Request returned failure 404")) {
                        onAbsent.accept(webHook);
                    }
                    return null;
                });
            }
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
    public User getSelfUser() {
        return getJda().getSelfUser();
    }

    @Override
    public JDA getShardById(int shardId) {
        return shardManager.getShardById(shardId);
    }

    @Override
    public Guild getGuildById(long guildId) {
        return shardManager.getGuildById(guildId);
    }

    @Override
    public JDA getShard(long guildId) {
        return shardManager.getShardById((int)((guildId >> 22) % shardsNum));
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

    @Scheduled(fixedDelay = 30000)
    public void tickPing() {
        if (MapUtils.isEmpty(pingCharts)) {
            return;
        }
        shardManager.getShards().forEach(e -> {
            TimeWindowChart reservoir = pingCharts.get(e);
            if (reservoir != null) {
                reservoir.update(JDA.Status.CONNECTED.equals(e.getStatus()) ? e.getPing() : -1);
            }
        });
    }

    @Override
    @CachedGauge(name = GAUGE_GUILDS, absolute = true, timeout = 1, timeoutUnit = TimeUnit.MINUTES)
    public long getGuildCount() {
        return shardManager != null ? shardManager.getGuildCache().size() : 0;
    }

    @Override
    @CachedGauge(name = GAUGE_USERS, absolute = true, timeout = 5, timeoutUnit = TimeUnit.MINUTES)
    public long getUserCount() {
        return shardManager != null ? shardManager.getUserCache().size() : 0;
    }

    @Override
    @CachedGauge(name = GAUGE_CHANNELS, absolute = true, timeout = 3, timeoutUnit = TimeUnit.MINUTES)
    public long getChannelCount() {
        return getTextChannelCount() + getVoiceChannelCount();
    }

    @Override
    @CachedGauge(name = GAUGE_TEXT_CHANNELS, absolute = true, timeout = 3, timeoutUnit = TimeUnit.MINUTES)
    public long getTextChannelCount() {
        return shardManager != null ? shardManager.getTextChannelCache().size() : 0;
    }

    @Override
    @CachedGauge(name = GAUGE_VOICE_CHANNELS, absolute = true, timeout = 3, timeoutUnit = TimeUnit.MINUTES)
    public long getVoiceChannelCount() {
        return shardManager != null ? shardManager.getVoiceChannelCache().size() : 0;
    }

    @Override
    @Gauge(name = GAUGE_PING, absolute = true)
    public double getAveragePing() {
        return shardManager != null ? shardManager.getAveragePing() : 0;
    }

    @Override
    public Map<JDA, TimeWindowChart> getPingCharts() {
        return Collections.unmodifiableMap(pingCharts);
    }

    @Override
    public String getUserId() {
        if (cachedUserId != null) {
            return cachedUserId;
        }

        int attempt = 0;
        while (cachedUserId == null && attempt++ < 5) {
            try {
                ResponseEntity<String> response = restTemplate.getForEntity(Requester.DISCORD_API_PREFIX + "/users/@me", String.class);
                if (!HttpStatus.OK.equals(response.getStatusCode())) {
                    LOGGER.warn("Could not get userId, endpoint returned {}", response.getStatusCode());
                    continue;
                }
                JSONObject object = new JSONObject(response.getBody());
                cachedUserId = object.getString("id");
                if (StringUtils.isNotEmpty(cachedUserId)) {
                    break;
                }
            } catch (Exception e) {
                // fall down
            }
            LOGGER.error("Could not request my own userId from Discord, will retry a few times");
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }

        if (cachedUserId == null) {
            throw new RuntimeException("Failed to retrieve my own userId from Discord");
        }
        return cachedUserId;
    }
}
