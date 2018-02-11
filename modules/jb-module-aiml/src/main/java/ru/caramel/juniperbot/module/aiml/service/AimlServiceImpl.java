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
package ru.caramel.juniperbot.module.aiml.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.Getter;
import lombok.Setter;
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
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Service
public class AimlServiceImpl implements AimlService, CommandHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AimlServiceImpl.class);

    @Value("${aiml.bots.path}")
    private String path;

    @Getter
    @Setter
    @Value("${aiml.bots.enabled:true}")
    private boolean enabled;

    @Getter
    private final Set<Long> ignoredGuilds = Collections.synchronizedSet(new HashSet<>());

    private final Map<String, Bot> bots = new ConcurrentHashMap<>();

    private Cache<User, Chat> sessions = CacheBuilder.newBuilder()
            .concurrencyLevel(4)
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build();

    @Autowired
    private CommandsService commandsService;

    @Autowired
    private ContextService contextService;

    @PostConstruct
    public void init() {
        commandsService.registerHandler(this);
    }

    private Bot createBot(String name) {
        return bots.computeIfAbsent(name, e -> {
            BotConfiguration configuration = BotConfiguration.builder()
                    .name(name)
                    .path(path)
                    .action("chat")
                    .build();
            return new Bot(configuration);
        });
    }

    @Override
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
    public void clear() {
        bots.clear();
        sessions.invalidateAll();
    }

    @Override
    public boolean handleMessage(MessageReceivedEvent event) {
        if (!enabled) {
            return false;
        }
        JDA jda = event.getJDA();
        if (event.getChannelType() != ChannelType.TEXT) {
            return false;
        }
        if (ignoredGuilds.contains(event.getGuild().getIdLong())) {
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

    @Override
    public int getPriority() {
        return Integer.MAX_VALUE;
    }
}
