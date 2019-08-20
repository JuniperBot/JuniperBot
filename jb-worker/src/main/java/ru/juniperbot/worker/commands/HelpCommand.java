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
package ru.juniperbot.worker.commands;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import ru.juniperbot.common.model.discord.GuildDto;
import ru.juniperbot.common.persistence.entity.CommandConfig;
import ru.juniperbot.common.persistence.entity.CustomCommand;
import ru.juniperbot.common.persistence.repository.CustomCommandRepository;
import ru.juniperbot.common.service.CommandConfigService;
import ru.juniperbot.common.service.GatewayService;
import ru.juniperbot.common.worker.command.model.AbstractCommand;
import ru.juniperbot.common.worker.command.model.BotContext;
import ru.juniperbot.common.worker.command.model.DiscordCommand;
import ru.juniperbot.common.worker.command.service.CommandsHolderService;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@DiscordCommand(
        key = "discord.command.help.key",
        description = "discord.command.help.desc",
        priority = 1
)
public class HelpCommand extends AbstractCommand {

    private static final String COMMON_GROUP = "discord.command.group.common";

    public static final String CUSTOM_GROUP = "discord.command.group.custom";

    @Autowired
    private CommandsHolderService holderService;

    @Autowired
    private CommandConfigService commandConfigService;

    @Autowired
    private CustomCommandRepository customCommandRepository;

    @Autowired
    private GatewayService gatewayService;

    @Override
    public boolean doCommand(GuildMessageReceivedEvent message, BotContext context, String query) {
        GuildDto guildDto = gatewayService.getGuildInfo(message.getGuild().getIdLong());

        boolean direct = Boolean.TRUE.equals(context.getConfig().getPrivateHelp());

        Map<String, CommandConfig> configMap = commandConfigService.findAllMap(context.getConfig().getGuildId());

        List<DiscordCommand> discordCommands = holderService.getCommands().entrySet().stream()
                .filter(e -> {
                    CommandConfig config = configMap.get(e.getValue().getClass()
                            .getAnnotation(DiscordCommand.class).key());
                    return commandsService.isApplicable(e.getValue(), config, message.getAuthor(), message.getMember(), message.getChannel())
                            && !commandsService.isRestricted(config, message.getChannel())
                            && !commandsService.isRestricted(config, message.getMember());

                })
                .map(e -> e.getValue().getClass().getAnnotation(DiscordCommand.class))
                .filter(e -> !e.hidden())
                .collect(Collectors.toList());

        Map<String, List<DiscordCommand>> groupedCommands = new TreeMap<>();
        for (DiscordCommand command : discordCommands) {
            for (String group : command.group()) {
                List<DiscordCommand> groupList = groupedCommands.computeIfAbsent(group, e -> new ArrayList<>());
                groupList.add(command);
            }
        }
        groupedCommands.forEach((k, v) -> v.sort(Comparator.comparingInt(DiscordCommand::priority)));

        Map<String, String> localizedGroups = groupedCommands.keySet().stream()
                .collect(Collectors.toMap(e -> e, e -> messageService.getMessageByLocale(e, context.getCommandLocale())));

        String rootGroup = COMMON_GROUP;
        if (StringUtils.isNotEmpty(query)) {
            for (Map.Entry<String, String> localized : localizedGroups.entrySet()) {
                if (Objects.equals(localized.getValue().toLowerCase(), query.toLowerCase())) {
                    rootGroup = localized.getKey();
                    break;
                }
            }
        }
        if (rootGroup == null || !groupedCommands.containsKey(rootGroup)) {
            messageService.onError(message.getChannel(), "discord.command.help.no-such-group");
            return false;
        }

        EmbedBuilder embedBuilder = getBaseEmbed(rootGroup);

        String prefix = context.getConfig().getPrefix();

        groupedCommands.remove(rootGroup).forEach(e -> embedBuilder.addField(
                prefix + messageService.getMessageByLocale(e.key(), context.getCommandLocale()),
                messageService.getMessage(e.description()), false));
        if (COMMON_GROUP.equals(rootGroup)) {
            groupedCommands.forEach((group, commands) -> {
                String groupTitle = messageService.getMessage(group);
                String groupTitleLocalized = messageService.getMessageByLocale(group, context.getCommandLocale());
                embedBuilder.addField(String.format("%s (%s%s %s):",
                        groupTitle,
                        prefix,
                        messageService.getMessageByLocale("discord.command.help.key", context.getCommandLocale()),
                        groupTitleLocalized.toLowerCase()),
                        commands.stream().map(e -> '`' + prefix + messageService.getMessageByLocale(e.key(),
                                context.getCommandLocale()) + '`')
                                .collect(Collectors.joining(" ")), false);
            });

            // Пользовательские команды
            List<CustomCommand> commands = customCommandRepository.findAllByGuildId(context.getConfig().getGuildId()).stream()
                    .filter(e -> {
                        CommandConfig config = e.getCommandConfig();
                        return config == null || !config.isDisabled()
                                && !commandsService.isRestricted(config, message.getChannel())
                                && !commandsService.isRestricted(config, message.getMember());
                    })
                    .collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(commands)) {
                StringBuilder list = new StringBuilder();
                commands.forEach(e -> {
                    if (list.length() > 0) {
                        list.append(", ");
                    }
                    list.append('`').append(context.getConfig().getPrefix()).append(e.getKey()).append('`');
                });
                if (list.length() > 0) {
                    embedBuilder.addField(messageService.getMessage(CUSTOM_GROUP) + ":",
                            list.toString(), false);
                }
            }
        }

        if (direct) {
            if (message.getAuthor() != null) {
                try {
                    message.getAuthor().openPrivateChannel()
                            .queue(channel -> send(message, channel, embedBuilder, true));
                } catch (Exception e) {
                    log.warn("Could not open private channel for {}", message.getAuthor(), e);
                }
            }
        } else {
            send(message, message.getChannel(), embedBuilder, false);
        }
        return true;
    }

    private void send(GuildMessageReceivedEvent message, MessageChannel channel, EmbedBuilder embedBuilder, boolean direct) {
        channel.sendMessage(embedBuilder.build()).queue();
        if (direct && message.getAuthor() != null) {
            contextService.withContext(message.getGuild(), () -> {
                messageService.onMessage(message.getChannel(), "discord.command.help.sent", message.getAuthor().getAsMention());
            });
        }
    }

    private EmbedBuilder getBaseEmbed(String group) {
        EmbedBuilder embedBuilder = messageService.getBaseEmbed(true)
                .setThumbnail(commonProperties.getBranding().getAvatarSmallUrl());
        if (COMMON_GROUP.equals(group)) {
            embedBuilder.setDescription(messageService.getMessage("discord.command.help.title"));
        } else {
            embedBuilder.setDescription(messageService.getMessage("discord.command.help.group.title", messageService.getMessage(group)));
        }
        return embedBuilder;
    }
}
