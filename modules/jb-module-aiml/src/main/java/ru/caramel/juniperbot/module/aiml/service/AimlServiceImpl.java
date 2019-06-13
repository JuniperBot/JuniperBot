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
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import org.apache.commons.lang3.StringUtils;
import org.goldrenard.jb.configuration.BotConfiguration;
import org.goldrenard.jb.core.Bot;
import org.goldrenard.jb.core.Chat;
import org.goldrenard.jb.model.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.caramel.juniperbot.core.command.model.CommandHandler;
import ru.caramel.juniperbot.core.command.service.CommandsService;
import ru.caramel.juniperbot.core.common.persistence.GuildConfig;
import ru.caramel.juniperbot.core.common.service.ConfigService;
import ru.caramel.juniperbot.core.event.service.ContextService;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class AimlServiceImpl implements AimlService, CommandHandler {

    @Value("${integrations.aiml.path:}")
    private String path;

    @Getter
    @Setter
    @Value("${integrations.aiml.enabled:true}")
    private boolean enabled;

    private final Map<String, Bot> bots = new ConcurrentHashMap<>();

    private Cache<User, Chat> sessions = CacheBuilder.newBuilder()
            .concurrencyLevel(4)
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build();

    @Autowired
    private ConfigService configService;

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
            log.error("Error creating session", e);
        }
        return null;
    }

    @Override
    public void clear() {
        bots.clear();
        sessions.invalidateAll();
    }

    @Override
    public boolean handleMessage(GuildMessageReceivedEvent event) {
        if (!enabled || StringUtils.isEmpty(path) || event.getAuthor() == null || event.getGuild() == null) {
            return false;
        }

        GuildConfig config = configService.get(event.getGuild());
        if (config == null
                || !config.isAssistantEnabled()
                || !event.getMessage().isMentioned(event.getJDA().getSelfUser())
                || !event.getGuild().getSelfMember().hasPermission(event.getChannel(), Permission.MESSAGE_WRITE)) {
            return false;
        }
        String content = event.getMessage().getContentRaw().trim();
        if (StringUtils.isEmpty(content)) {
            return false;
        }

        JDA jda = event.getJDA();
        boolean usingMention;
        String mention = jda.getSelfUser().getAsMention();
        if (!(usingMention = content.startsWith(mention))) {
            mention = String.format("<@!%s>", jda.getSelfUser().getId());
            usingMention = content.startsWith(mention);
        }
        if (usingMention) {
            String input = content.substring(mention.length()).trim();
            if (StringUtils.isNotBlank(input)) {
                contextService.withContextAsync(event.getGuild(), () -> {
                    event.getChannel().sendTyping().queue();
                    Chat chatSession = getSession("juniper_en", event.getAuthor());
                    if (chatSession != null) {
                        String respond = chatSession.multisentenceRespond(createRequest(event, input));
                        if (StringUtils.isNotBlank(respond)) {
                            event.getChannel().sendMessage(respond).queue();
                        }
                    }
                });
                return true;
            }
        }
        return false;
    }

    private Request createRequest(GuildMessageReceivedEvent event, String input) {
        Request.RequestBuilder builder = Request.builder()
                .input(input)
                .attribute("dMessageEvent", event)
                .attribute("dAuthorName", event.getAuthor().getName())
                .attribute("dAuthorMention", event.getAuthor().getAsMention())
                .attribute("dAuthorAvatarUrl", event.getAuthor().getEffectiveAvatarUrl());

        if (event.getMember() != null) {
            Member member = event.getMember();
            builder
                    .attribute("dAuthorName", member.getEffectiveName())
                    .attribute("dAuthorMention", member.getAsMention());
        }

        if (event.getGuild() != null) {
            Guild guild = event.getGuild();
            builder
                    .attribute("dGuildName", guild.getName())
                    .attribute("dGuildOwnerName", guild.getOwner().getEffectiveName());
        }

        if (event.getChannel() != null) {
            TextChannel channel = event.getChannel();
            builder.attribute("dChannelName", channel.getName());
        }
        return builder.build();
    }

    @Override
    public int getPriority() {
        return Integer.MAX_VALUE;
    }
}
