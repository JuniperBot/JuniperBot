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
package ru.caramel.juniperbot.service.impl;

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.PropertyPlaceholderHelper;
import ru.caramel.juniperbot.model.Command;
import ru.caramel.juniperbot.model.BotContext;
import ru.caramel.juniperbot.model.exception.ValidationException;
import ru.caramel.juniperbot.configuration.DiscordConfig;
import ru.caramel.juniperbot.integration.discord.model.DiscordException;
import ru.caramel.juniperbot.model.dto.CustomCommandDto;
import ru.caramel.juniperbot.persistence.entity.CustomCommand;
import ru.caramel.juniperbot.persistence.entity.GuildConfig;
import ru.caramel.juniperbot.persistence.repository.CustomCommandRepository;
import ru.caramel.juniperbot.service.*;
import ru.caramel.juniperbot.utils.MapPlaceholderResolver;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CommandsServiceImpl implements CommandsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandsServiceImpl.class);

    @Autowired
    private DiscordConfig config;

    @Autowired
    private ConfigService configService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private CustomCommandRepository commandRepository;

    @Autowired
    private MapperService mapperService;

    @Autowired
    private CommandsHolderService commandsHolderService;

    private static PropertyPlaceholderHelper placeholderHelper = new PropertyPlaceholderHelper("{", "}");

    private Map<MessageChannel, BotContext> contexts = new HashMap<>();

    @Override
    @Transactional
    @Async("commandsExecutor")
    public void onMessageReceived(MessageReceivedEvent event) {
        JDA jda = event.getJDA();
        if (event.getAuthor().isBot()) {
            return;
        }
        GuildConfig guildConfig = null;
        if (event.getChannelType().isGuild() && event.getGuild() != null) {
            guildConfig = configService.getOrCreate(event.getGuild());
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
            sendCommand(event, input, inlinePrefix, guildConfig, false);
        }
    }

    private void sendCommand(MessageReceivedEvent event, String content, String prefix, GuildConfig guildConfig, boolean alias) {
        String[] args = content.split("\\s+");
        if (args.length == 0) {
            return;
        }
        content = content.substring(args[0].length(), content.length()).trim();

        Command command = commandsHolderService.getByLocale(args[0]);
        if (command != null && !command.isApplicable(event.getChannel(), guildConfig)) {
            return;
        }
        if (command == null) {
            if (!alias && guildConfig != null) {
                sendCustomCommand(event, content, args[0], prefix, guildConfig);
            }
            return;
        }

        BotContext context = contexts.computeIfAbsent(event.getChannel(), e -> new BotContext());
        context.setPrefix(prefix);
        context.setConfig(guildConfig);
        context.setGuild(event.getGuild());
        try {
            command.doCommand(event, context, content);
        } catch (ValidationException e) {
            messageService.onError(event.getChannel(), e.getMessage(), e.getArgs());
        } catch (DiscordException e) {
            messageService.onError(event.getChannel(),
                    messageService.hasMessage(e.getMessage()) ? e.getMessage() : "discord.global.error");
            LOGGER.error("Command {} execution error", args[0], e);
        }
    }

    private void sendCustomCommand(MessageReceivedEvent event, String content, String key, String prefix, GuildConfig config) {
        CustomCommand command = commandRepository.findByKeyAndConfig(key, config);
        if (command == null) {
            return;
        }
        String commandContent = placeholderHelper.replacePlaceholders(command.getContent(), getResolver(event, content));
        switch (command.getType()) {
            case ALIAS:
                sendCommand(event, commandContent, prefix, config, true);
                break;
            case MESSAGE:
                messageService.sendMessageSilent(event.getChannel()::sendMessage, commandContent);
                break;
        }
    }

    private MapPlaceholderResolver getResolver(MessageReceivedEvent event, String content) {
        MapPlaceholderResolver resolver = new MapPlaceholderResolver();
        resolver.put(messageService.getMessage("custom.commands.placeholder.author"), event.getAuthor().getAsMention());
        resolver.put(messageService.getMessage("custom.commands.placeholder.guild"), event.getGuild().getName());
        resolver.put(messageService.getMessage("custom.commands.placeholder.content"), content);
        return resolver;
    }

    @Transactional
    public void saveCommands(List<CustomCommandDto> commands, long serverId) {
        GuildConfig config = configService.getOrCreate(serverId);
        List<CustomCommand> customCommands = config.getCommands() != null ? config.getCommands() : new ArrayList<>();
        if (commands == null) {
            commands = Collections.emptyList();
        }

        List<CustomCommand> result = new ArrayList<>();
        // adding new commands
        result.addAll(mapperService.getCommands(commands.stream().filter(e -> e.getId() == null).collect(Collectors.toList())));
        result.forEach(e -> e.setConfig(config));

        // update existing
        commands.stream().filter(e -> e.getId() != null).forEach(e -> {
            CustomCommand command = customCommands.stream().filter(e1 -> Objects.equals(e1.getId(), e.getId())).findFirst().orElse(null);
            if (command != null) {
                mapperService.updateCommand(e, command);
                result.add(command);
            }
        });

        config.setCommands(result);
        commandRepository.save(result);

        // delete old
        commandRepository.delete(customCommands.stream().filter(e -> !result.contains(e)).collect(Collectors.toList()));
    }
}
