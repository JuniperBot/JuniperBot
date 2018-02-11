package ru.caramel.juniperbot.module.aiml.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.alicebot.ab.Bot;
import org.alicebot.ab.Chat;
import org.alicebot.ab.configuration.BotConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.caramel.juniperbot.core.service.CommandHandler;
import ru.caramel.juniperbot.core.service.CommandsService;
import ru.caramel.juniperbot.core.service.ContextService;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Service
public class AimlService implements CommandHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AimlService.class);

    @Value("${aiml.bots.path}")
    private String path;

    @Autowired
    private CommandsService commandsService;

    @Autowired
    private ContextService contextService;

    private final Map<String, Bot> bots = new ConcurrentHashMap<>();

    private Cache<User, Chat> sessions = CacheBuilder.newBuilder()
            .concurrencyLevel(4)
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build();

    @PostConstruct
    public void init() {
        commandsService.registerHandler(this);
    }

    public Bot createBot(String name) {
        BotConfiguration configuration = BotConfiguration.builder()
                .name(name)
                .path(path)
                .action("chat")
                .build();
        return bots.computeIfAbsent(name, e -> new Bot(configuration));
    }

    public Chat getSession(String botName, User user) {
        Bot bot = createBot(botName);
        try {
            return sessions.get(user, () -> new Chat(bot, false, user.getId()));
        } catch (ExecutionException e) {
            LOGGER.error("Error creating session", e);
        }
        return null;
    }

    @Override
    public boolean handleMessage(MessageReceivedEvent event) {
        JDA jda = event.getJDA();
        if (event.getChannelType() != ChannelType.TEXT) {
            return false;
        }
        if (!event.getMessage().isMentioned(event.getJDA().getSelfUser())) {
            return false;
        }
        String content = event.getMessage().getContentRaw().trim();
        if (StringUtils.isEmpty(content)) {
            return false;
        }

        boolean usingMention;
        String mention = jda.getSelfUser().getAsMention();
        if (!(usingMention = content.startsWith(mention))) {
            mention = String.format("<@!%s>", jda.getSelfUser().getId());
            usingMention = content.startsWith(mention);
        }
        if (usingMention) {
            String input = content.substring(mention.length()).trim();
            if (StringUtils.isNotBlank(input)) {
                contextService.execute(event.getGuild(), () -> {
                    event.getChannel().sendTyping().complete();
                    Chat chatSession = getSession("juniper_en", event.getAuthor());
                    if (chatSession != null) {
                        String respond = chatSession.multisentenceRespond(input);
                        event.getChannel().sendMessage(respond).queue();
                    }
                });
                return true;
            }
        }
        return false;
    }

    public int getPriority() {
        return Integer.MAX_VALUE;
    }
}
