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
package ru.caramel.juniperbot.commands.common;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import ru.caramel.juniperbot.commands.Command;
import ru.caramel.juniperbot.commands.model.BotContext;
import ru.caramel.juniperbot.commands.model.CommandGroup;
import ru.caramel.juniperbot.commands.model.DiscordCommand;
import ru.caramel.juniperbot.configuration.DiscordConfig;
import ru.caramel.juniperbot.integration.discord.DiscordClient;
import ru.caramel.juniperbot.persistence.entity.CustomCommand;
import ru.caramel.juniperbot.service.CommandsHolderService;
import ru.caramel.juniperbot.service.MessageService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@DiscordCommand(key = "discord.command.help.key", description = "discord.command.help.desc", priority = 1)
public class HelpCommand implements Command {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiscordClient.class);

    @Autowired
    private DiscordConfig discordConfig;

    @Autowired
    private CommandsHolderService holderService;

    @Autowired
    private MessageService messageService;

    @Override
    public boolean doCommand(MessageReceivedEvent message, BotContext context, String query) {
        boolean direct = context.getConfig() != null && Boolean.TRUE.equals(context.getConfig().getPrivateHelp());

        List<DiscordCommand> discordCommands = holderService.getCommands().entrySet().stream()
                .filter(e -> e.getValue().isApplicable(message.getChannel(), context.getConfig()))
                .map(e -> e.getValue().getClass().getAnnotation(DiscordCommand.class))
                .filter(e -> !e.hidden())
                .collect(Collectors.toList());

        discordCommands.sort(Comparator.comparingInt(DiscordCommand::priority));

        Map<CommandGroup, List<DiscordCommand>> groupedCommands = discordCommands
                .stream().collect(Collectors.groupingBy(DiscordCommand::group));

        CommandGroup rootGroup = CommandGroup.COMMON;
        if (StringUtils.isNotEmpty(query)) {
            rootGroup = messageService.getEnumeration(CommandGroup.class, query);
        }
        if (rootGroup == null || !groupedCommands.containsKey(rootGroup)) {
            messageService.onError(message.getChannel(), "discord.command.help.no-such-group");
            return false;
        }

        List<EmbedBuilder> messages = new ArrayList<>();
        EmbedBuilder embedBuilder = getBaseEmbed(rootGroup, message);
        messages.add(embedBuilder);

        groupedCommands.remove(rootGroup).forEach(e -> embedBuilder.addField(
                context.getPrefix() + messageService.getMessage(e.key()),
                messageService.getMessage(e.description()), false));
        if (CommandGroup.COMMON.equals(rootGroup)) {
            groupedCommands.forEach((group, commands) -> {
                if (direct) {
                    EmbedBuilder groupBuilder = getBaseEmbed(group, message);
                    commands.forEach(e -> groupBuilder.addField(
                            context.getPrefix() + messageService.getMessage(e.key()),
                            messageService.getMessage(e.description()), false));
                    messages.add(groupBuilder);
                } else {
                    String groupTitle = messageService.getEnumTitle(group);
                    embedBuilder.addField(String.format("%s (%s%s %s):",
                            groupTitle,
                            context.getPrefix(),
                            messageService.getMessage("discord.command.help.key"),
                            groupTitle.toLowerCase()),
                            commands.stream().map(e -> '`' + context.getPrefix() + messageService.getMessage(e.key()) + '`')
                                    .collect(Collectors.joining(", ")), false);
                }
            });

            // Пользовательские команды
            if (message.getChannelType().isGuild() && context.getConfig() != null) {
                List<CustomCommand> commands = context.getConfig().getCommands();
                if (CollectionUtils.isNotEmpty(commands)) {
                    StringBuilder list = new StringBuilder();
                    commands.forEach(e -> {
                        if (list.length() > 0) {
                            list.append(", ");
                        }
                        list.append('`').append(context.getPrefix()).append(e.getKey()).append('`');
                    });
                    if (list.length() > 0) {
                        embedBuilder.addField(messageService.getEnumTitle(CommandGroup.CUSTOM) + ":", list.toString(), false);
                    }
                }
            }
        }

        MessageChannel channel = null;
        if (direct) {
            if (message.getAuthor() != null) {
                try {
                    channel = message.getAuthor().openPrivateChannel().complete();
                } catch (Exception e) {
                    LOGGER.warn("Could not open private channel for {}", message.getAuthor(), e);
                }
            }
        } else {
            channel = message.getChannel();
        }
        if (channel == null) {
            return false;
        }
        for (EmbedBuilder builder : messages) {
            channel.sendMessage(builder.build()).queue();
        }
        if (direct && message.getAuthor() != null) {
            messageService.onMessage(message.getChannel(), "discord.command.help.sent", message.getAuthor().getAsMention());
        }
        return true;
    }

    private EmbedBuilder getBaseEmbed(CommandGroup group, MessageReceivedEvent message) {
        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setThumbnail(message.getJDA().getSelfUser().getAvatarUrl())
                .setColor(discordConfig.getAccentColor());
        if (CommandGroup.COMMON.equals(group)) {
            embedBuilder.setDescription(messageService.getMessage("discord.command.help.title"));
        } else {
            embedBuilder.setDescription(messageService.getMessage("discord.command.help.group.title", messageService.getEnumTitle(group)));
        }
        if (StringUtils.isNotEmpty(discordConfig.getCopyContent())) {
            embedBuilder.setFooter(discordConfig.getCopyContent(), discordConfig.getCopyImageUrl());
        }
        return embedBuilder;
    }
}
