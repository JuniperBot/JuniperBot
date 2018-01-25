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

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
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

import java.util.HashMap;
import java.util.Map;

@Service
public class CommandsServiceImpl implements CommandsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandsServiceImpl.class);

    @Autowired
    private ConfigService configService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private CommandsHolderService commandsHolderService;

    private Map<MessageChannel, BotContext> contexts = new HashMap<>();

    @Override
    @Transactional
    public void onMessageReceived(MessageReceivedEvent event) {
        sendMessage(event, this);
    }

    @Override
    public void sendMessage(MessageReceivedEvent event, MessageSender sender) {
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
        if (!commandsHolderService.isAnyCommand(firstPart)) {
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
        if (command != null && !command.isApplicable(event.getChannel(), guildConfig)) {
            return;
        }
        if (command == null) {
            return;
        }

        BotContext context = contexts.computeIfAbsent(event.getChannel(), e -> new BotContext());
        context.setConfig(guildConfig);
        context.setGuild(event.getGuild());
        try {
            command.doCommand(event, context, content);
        } catch (ValidationException e) {
            messageService.onError(event.getChannel(), e.getMessage(), e.getArgs());
        } catch (DiscordException e) {
            messageService.onError(event.getChannel(),
                    messageService.hasMessage(e.getMessage()) ? e.getMessage() : "discord.global.error");
            LOGGER.error("Command {} execution error", key, e);
        }
    }
}
