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
package ru.caramel.juniperbot.core.commands;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import ru.caramel.juniperbot.core.model.AbstractCommand;
import ru.caramel.juniperbot.core.model.BotContext;
import ru.caramel.juniperbot.core.model.CommandExtension;
import ru.caramel.juniperbot.core.model.DiscordCommand;
import ru.caramel.juniperbot.core.model.enums.CommandGroup;
import ru.caramel.juniperbot.core.service.CommandsHolderService;
import ru.caramel.juniperbot.core.service.ConfigService;
import ru.caramel.juniperbot.core.service.MessageService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@DiscordCommand(key = "discord.command.help.key", description = "discord.command.help.desc", priority = 1)
public class HelpCommand extends AbstractCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(HelpCommand.class);

    @Autowired
    private CommandsHolderService holderService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private ConfigService configService;

    @Autowired(required = false)
    private List<CommandExtension> extensions;

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

        String prefix = context.getConfig() != null ? context.getConfig().getPrefix() : configService.getDefaultPrefix();

        groupedCommands.remove(rootGroup).forEach(e -> embedBuilder.addField(
                prefix + messageService.getMessage(e.key()),
                messageService.getMessage(e.description()), false));
        if (CommandGroup.COMMON.equals(rootGroup)) {
            groupedCommands.forEach((group, commands) -> {
                if (direct) {
                    EmbedBuilder groupBuilder = getBaseEmbed(group, message);
                    commands.forEach(e -> groupBuilder.addField(
                            prefix + messageService.getMessage(e.key()),
                            messageService.getMessage(e.description()), false));
                    messages.add(groupBuilder);
                } else {
                    String groupTitle = messageService.getEnumTitle(group);
                    embedBuilder.addField(String.format("%s (%s%s %s):",
                            groupTitle,
                            prefix,
                            messageService.getMessage("discord.command.help.key"),
                            groupTitle.toLowerCase()),
                            commands.stream().map(e -> '`' + prefix + messageService.getMessage(e.key()) + '`')
                                    .collect(Collectors.joining(", ")), false);
                }
            });

            if (CollectionUtils.isNotEmpty(extensions)) {
                for (CommandExtension extension : extensions) {
                    extension.extendHelp(message, context, embedBuilder);
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
        EmbedBuilder embedBuilder = messageService.getBaseEmbed(true)
                .setThumbnail(message.getJDA().getSelfUser().getAvatarUrl());
        if (CommandGroup.COMMON.equals(group)) {
            embedBuilder.setDescription(messageService.getMessage("discord.command.help.title"));
        } else {
            embedBuilder.setDescription(messageService.getMessage("discord.command.help.group.title", messageService.getEnumTitle(group)));
        }
        return embedBuilder;
    }
}
