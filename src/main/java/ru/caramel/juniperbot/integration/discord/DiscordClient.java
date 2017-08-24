package ru.caramel.juniperbot.integration.discord;

import lombok.Getter;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.ExceptionEvent;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
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
import ru.caramel.juniperbot.commands.Command;
import ru.caramel.juniperbot.commands.model.BotContext;
import ru.caramel.juniperbot.commands.model.DiscordCommand;
import ru.caramel.juniperbot.commands.model.ValidationException;
import ru.caramel.juniperbot.configuration.DiscordConfig;
import ru.caramel.juniperbot.integration.discord.model.DiscordEvent;
import ru.caramel.juniperbot.integration.discord.model.DiscordException;
import ru.caramel.juniperbot.integration.discord.model.WebHookMessage;
import ru.caramel.juniperbot.persistence.entity.GuildConfig;
import ru.caramel.juniperbot.persistence.entity.WebHook;
import ru.caramel.juniperbot.service.ConfigService;
import ru.caramel.juniperbot.service.MessageService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.security.auth.login.LoginException;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
public class DiscordClient extends ListenerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiscordClient.class);

    private static Set<String> musicChannels = new HashSet<>(Arrays.asList("музыка", "music"));

    @Autowired
    private DiscordConfig config;

    @Autowired
    private DiscordBeanProviders discordBeanProviders;

    @Autowired
    private ConfigService configService;

    @Autowired
    private MessageService messageService;

    @Getter
    private JDA jda;

    @Getter
    private Map<String, Command> commands;

    private Map<MessageChannel, BotContext> contexts = new HashMap<>();

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

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

    @Override
    public void onReady(ReadyEvent event) {
        if (StringUtils.isNotEmpty(config.getPlayingStatus())) {
            jda.getPresence().setGame(Game.of(config.getPlayingStatus()));
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) {
            return;
        }
        discordBeanProviders.setMessageContext(event);
        log(event);

        GuildConfig guildConfig = null;
        if (event.getChannelType().isGuild() && event.getGuild() != null) {
            guildConfig = configService.getOrCreate(event.getGuild().getIdLong());
        }

        String content = event.getMessage().getRawContent().trim();
        String inlinePrefix = guildConfig != null ? guildConfig.getPrefix() : config.getPrefix();
        String rawPrefix = inlinePrefix;
        if (event.getMessage().isMentioned(jda.getSelfUser())) {
            String customMention = String.format("<@!%s>", jda.getSelfUser().getId());
            rawPrefix = content.startsWith(customMention) ? customMention : jda.getSelfUser().getAsMention();
        }
        if (StringUtils.isNotEmpty(content) && content.startsWith(rawPrefix) && content.length() <= 255) {
            String input = content.substring(rawPrefix.length()).trim();
            sendCommand(event, input, inlinePrefix, guildConfig);
        }
    }

    private void sendCommand(MessageReceivedEvent event, String content, String prefix, GuildConfig guildConfig) {
        String[] args = content.split("\\s+");
        if (args.length == 0) {
            return;
        }

        Command command = commands.get(args[0]);
        if (command == null || !command.isApplicable(event.getChannel())) {
            return;
        }
        BotContext context = contexts.computeIfAbsent(event.getChannel(), e -> new BotContext());
        context.setPrefix(prefix);
        context.setConfig(guildConfig);
        context.setGuild(event.getGuild());
        try {
            content = content.substring(args[0].length(), content.length()).trim();
            command.doCommand(event, context, content);
        } catch (ValidationException e) {
            messageService.onError(event.getChannel(), e.getMessage());
        } catch (DiscordException e) {
            messageService.onError(event.getChannel(), "Ой, произошла какая-то ошибка :C Покорми меня?");
            LOGGER.error("Command {} execution error", args[0], e);
        }
    }

    @Autowired
    private void registerCommands(List<Command> commands) {
        this.commands = commands.stream()
                .filter(e -> e.getClass().isAnnotationPresent(DiscordCommand.class))
                .collect(Collectors.toMap(e -> e.getClass().getAnnotation(DiscordCommand.class).key(), e -> e));
    }

    public boolean executeWebHook(WebHook webHook, WebHookMessage message, Consumer<WebHook> onAbsent) {
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

    private void log(MessageReceivedEvent event) {
        switch (event.getChannelType()) {
            case PRIVATE:
                LOGGER.debug("[PM] {}: {}", event.getAuthor().getName(), event.getMessage().getContent());
                break;
            case TEXT:
                LOGGER.debug("[{}][{}] {}: {}", event.getGuild().getName(),
                        event.getChannel().getName(), event.getMember() != null ? event.getMember().getEffectiveName() : "WebHook",
                        event.getMessage().getContent());
                break;
            default:
                break;
        }
    }

    @Override
    public void onException(ExceptionEvent event) {
        LOGGER.error("JDA error", event.getCause());
    }

    @Override
    public void onGenericEvent(Event event) {
        applicationEventPublisher.publishEvent(new DiscordEvent(event));
    }

    @PreDestroy
    public void destroy() {
        jda.shutdownNow();
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
        for (String name : musicChannels) {
            channel = guild.getVoiceChannelsByName(name, true).stream().findAny().orElse(null);
            if (channel != null) {
                return channel;
            }
        }
        return guild.getVoiceChannels().stream().findAny().orElse(null);
    }
}
