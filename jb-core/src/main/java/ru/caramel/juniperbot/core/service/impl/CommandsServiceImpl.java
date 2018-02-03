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

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.utils.PermissionUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.caramel.juniperbot.core.model.BotContext;
import ru.caramel.juniperbot.core.model.Command;
import ru.caramel.juniperbot.core.model.exception.DiscordException;
import ru.caramel.juniperbot.core.model.exception.ValidationException;
import ru.caramel.juniperbot.core.persistence.entity.GuildConfig;
import ru.caramel.juniperbot.core.service.*;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class CommandsServiceImpl implements CommandsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandsServiceImpl.class);

    @Autowired
    private ConfigService configService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private CommandsHolderService commandsHolderService;

    @Autowired
    private StatisticsService statisticsService;

    private Map<MessageChannel, BotContext> contexts = new HashMap<>();

    private Meter executions;

    private Counter counter;

    @PostConstruct
    public void init() {
        executions = statisticsService.getMeter(EXECUTIONS_METER);
        counter = statisticsService.getCounter(EXECUTIONS_COUNTER);
    }

    @Override
    @Transactional
    public void onMessageReceived(MessageReceivedEvent event) {
        sendMessage(event, this, commandsHolderService::isAnyCommand);
    }

    @Override
    public void sendMessage(MessageReceivedEvent event, MessageSender sender, Function<String, Boolean> commandCheck) {
        JDA jda = event.getJDA();
        if (event.getAuthor().isBot()) {
            return;
        }
        String content = event.getMessage().getContentRaw().trim();
        if (StringUtils.isEmpty(content)) {
            return;
        }

        String prefix = null;
        String input = content;
        boolean usingMention = false;

        if (event.getMessage().isMentioned(jda.getSelfUser())) {
            String mention = jda.getSelfUser().getAsMention();
            if (!(usingMention = content.startsWith(mention))) {
                mention = String.format("<@!%s>", jda.getSelfUser().getId());
                usingMention = content.startsWith(mention);

            }
            if (usingMention) {
                prefix = mention;
                input = content.substring(prefix.length()).trim();
            }
        }

        String firstPart = input.split("\\s+", 2)[0].trim();
        if (commandCheck != null && !commandCheck.apply(firstPart)) {
            return;
        }

        GuildConfig guildConfig = null;
        if (event.getChannelType().isGuild() && event.getGuild() != null) {
            guildConfig = configService.getOrCreate(event.getGuild());
        }

        if (!usingMention) {
            prefix = guildConfig != null ? guildConfig.getPrefix() : configService.getDefaultPrefix();
            input = content.substring(prefix.length()).trim();
        }
        if (content.startsWith(prefix)) {
            String[] args = input.split("\\s+", 2);
            input = args.length > 1 ? args[1] : "";
            sender.sendCommand(event, input, args[0], guildConfig);
        }
    }

    @Override
    public void sendCommand(MessageReceivedEvent event, String content, String key, GuildConfig guildConfig) {
        Command command = commandsHolderService.getByLocale(key);
        if (command != null && !command.isApplicable(event, guildConfig)) {
            return;
        }
        if (command == null) {
            return;
        }

        if (event.getChannelType().isGuild()) {
            Permission[] permissions = command.getPermissions();
            if (permissions != null && permissions.length > 0) {
                Member self = event.getGuild().getSelfMember();
                if (self != null && !PermissionUtil.checkPermission(event.getTextChannel(), self, permissions)) {
                    String list = Stream.of(permissions)
                            .filter(e -> !PermissionUtil.checkPermission(event.getTextChannel(), self, e))
                            .map(e -> messageService.getEnumTitle(e))
                            .collect(Collectors.joining("\n"));
                    if (PermissionUtil.checkPermission(event.getTextChannel(), self, Permission.MESSAGE_WRITE)) {
                        messageService.onError(event.getChannel(), "discord.command.insufficient.permissions", list);
                    }
                    return;
                }
            }
        }

        BotContext context = contexts.computeIfAbsent(event.getChannel(), e -> new BotContext());
        context.setConfig(guildConfig);
        context.setGuild(event.getGuild());
        try {
            command.doCommand(event, context, content);
            counter.inc();
        } catch (ValidationException e) {
            messageService.onError(event.getChannel(), e.getMessage(), e.getArgs());
        } catch (DiscordException e) {
            messageService.onError(event.getChannel(),
                    messageService.hasMessage(e.getMessage()) ? e.getMessage() : "discord.global.error");
            LOGGER.error("Command {} execution error", key, e);
        } finally {
            executions.mark();
        }
    }
}
