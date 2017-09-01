package ru.caramel.juniperbot.integration.discord;

import lombok.Getter;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.*;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import ru.caramel.juniperbot.configuration.DiscordConfig;
import ru.caramel.juniperbot.integration.discord.model.DiscordEvent;
import ru.caramel.juniperbot.integration.discord.model.WebHookMessage;
import ru.caramel.juniperbot.persistence.entity.WebHook;
import ru.caramel.juniperbot.service.MessageService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.security.auth.login.LoginException;
import java.util.function.Consumer;

@Service
public class DiscordClient extends ListenerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiscordClient.class);

    @Autowired
    private DiscordConfig config;

    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    private MessageService messageService;

    @Getter
    private JDA jda;

    @PostConstruct
    public void init() {
        try {
            jda = new JDABuilder(config.getAccountType())
                    .setToken(config.getToken())
                    .addEventListener(this)
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
        if (StringUtils.isNotEmpty(config.getPlayingStatus())) {
            jda.getPresence().setGame(Game.of(config.getPlayingStatus()));
        }
    }

    @Override
    public void onException(ExceptionEvent event) {
        LOGGER.error("JDA error", event.getCause());
    }

    public boolean executeWebHook(WebHook webHook, WebHookMessage message, Consumer<WebHook> onAbsent) {
        if (message.isEmpty()) {
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

    public boolean isConnected() {
        return jda != null && JDA.Status.CONNECTED.equals(jda.getStatus());
    }

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
