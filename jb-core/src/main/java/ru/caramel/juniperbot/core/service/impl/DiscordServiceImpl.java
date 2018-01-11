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
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.audio.factory.IAudioSendFactory;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.ExceptionEvent;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.exceptions.ErrorResponseException;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.requests.Request;
import net.dv8tion.jda.core.requests.Response;
import net.dv8tion.jda.core.requests.RestAction;
import net.dv8tion.jda.core.requests.Route;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import ru.caramel.juniperbot.core.model.DiscordEvent;
import ru.caramel.juniperbot.core.modules.webhook.model.WebHookMessage;
import ru.caramel.juniperbot.core.modules.webhook.persistence.entity.WebHook;
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

    @Value("${discord.client.token}")
    private String token;

    @Value("${discord.client.accountType:BOT}")
    private AccountType accountType;

    @Value("${discord.client.playingStatus}")
    private String playingStatus;

    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    private MessageService messageService;

    @Autowired
    private IAudioSendFactory audioSendFactory;

    @Getter
    private JDA jda;

    @PostConstruct
    public void init() {
        Objects.requireNonNull(token, "No Discord Token specified");
        try {
            jda = new JDABuilder(accountType)
                    .setToken(token)
                    .addEventListener(this)
                    .setAudioSendFactory(audioSendFactory)
                    .buildAsync();
        } catch (LoginException e) {
            LOGGER.error("Could not login user with specified token", e);
        } catch (RateLimitedException e) {
            throw new RuntimeException(e);
        }
    }

    @PreDestroy
    public void destroy() {
        jda.shutdownNow();
    }

    @Override
    public void onGenericEvent(Event event) {
        publisher.publishEvent(new DiscordEvent(event));
    }

    @Override
    public void onReady(ReadyEvent event) {
        if (StringUtils.isNotEmpty(playingStatus)) {
            jda.getPresence().setGame(Game.playing(playingStatus));
        }
    }

    @Override
    public void onException(ExceptionEvent event) {
        LOGGER.error("JDA error", event.getCause());
    }

    @Override
    public boolean executeWebHook(WebHook webHook, WebHookMessage message, Consumer<WebHook> onAbsent) {
        if (message == null || message.isEmpty()) {
            return false;
        }
        JSONObject obj = message.toJSONObject();
        RestAction<JSONObject> action = new RestAction<JSONObject>(jda, Route.Custom.POST_ROUTE.compile(String.format("webhooks/%s/%s", webHook.getHookId(), webHook.getToken())), obj) {

            @SuppressWarnings("unchecked")
            @Override
            protected void handleResponse(Response response, Request request) {
                if (response.isOk()) {
                    request.onSuccess(null);
                } else {
                    request.onFailure(response);
                    if (response.code == 404) {
                        onAbsent.accept(webHook);
                    }
                }
            }
        };

        try {
            action.queue();
            return false;
        } catch (ErrorResponseException e) {
            LOGGER.error("Can't execute webhook: ", e);
        }
        return true;
    }

    @Override
    public boolean isConnected() {
        return jda != null && JDA.Status.CONNECTED.equals(jda.getStatus());
    }

    @Override
    public VoiceChannel getDefaultMusicChannel(long guildId) {
        if (!isConnected()) {
            return null;
        }
        Guild guild = jda.getGuildById(guildId);
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
