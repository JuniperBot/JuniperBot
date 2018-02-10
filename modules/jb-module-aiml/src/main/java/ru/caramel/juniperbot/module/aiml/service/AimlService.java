package ru.caramel.juniperbot.module.aiml.service;

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.alicebot.ab.Bot;
import org.alicebot.ab.Chat;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.caramel.juniperbot.core.service.CommandHandler;
import ru.caramel.juniperbot.core.service.CommandsService;
import ru.caramel.juniperbot.core.service.ContextService;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AimlService implements CommandHandler {

    @Value("${aiml.bots.path}")
    private String path;

    @Autowired
    private CommandsService commandsService;

    @Autowired
    private ContextService contextService;

    private final Map<String, Bot> bots = new ConcurrentHashMap<>();

    private final Map<User, Chat> sessions = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        commandsService.registerHandler(this);
    }

    public Bot createBot(String name) {
        return bots.computeIfAbsent(name, e -> new Bot(e, path, "chat"));
    }

    public Chat getSession(String botName, User user) {
        Bot bot = createBot(botName);
        return sessions.computeIfAbsent(user, e -> new Chat(bot, false, user.getId()));
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
                    String respond = chatSession.multisentenceRespond(input);
                    event.getChannel().sendMessage(respond).queue();
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
