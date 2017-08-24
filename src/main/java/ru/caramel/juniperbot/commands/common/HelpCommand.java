package ru.caramel.juniperbot.commands.common;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import ru.caramel.juniperbot.commands.Command;
import ru.caramel.juniperbot.commands.model.CommandGroup;
import ru.caramel.juniperbot.commands.model.DiscordCommand;
import ru.caramel.juniperbot.configuration.DiscordConfig;
import ru.caramel.juniperbot.integration.discord.DiscordClient;
import ru.caramel.juniperbot.commands.model.BotContext;
import ru.caramel.juniperbot.service.MessageService;

import java.util.*;
import java.util.stream.Collectors;

@DiscordCommand(key = "хелп", description = "Отображает эту справку")
public class HelpCommand implements Command {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiscordClient.class);

    @Autowired
    private DiscordConfig discordConfig;

    @Autowired
    private DiscordClient discordClient;

    @Autowired
    private MessageService messageService;

    @Override
    public boolean doCommand(MessageReceivedEvent message, BotContext context, String query) {
        boolean direct = context.getConfig() != null && Boolean.TRUE.equals(context.getConfig().getPrivateHelp());

        List<DiscordCommand> discordCommands = discordClient.getCommands().entrySet().stream()
                .filter(e -> e.getValue().isApplicable(message.getChannel()))
                .map(e -> e.getValue().getClass().getAnnotation(DiscordCommand.class))
                .filter(e -> !e.hidden())
                .collect(Collectors.toList());

        Map<CommandGroup, List<DiscordCommand>> groupedCommands = discordCommands
                .stream().collect(Collectors.groupingBy(DiscordCommand::group));
        groupedCommands.forEach((k, v) -> v.sort(Comparator.comparing(DiscordCommand::key)));

        CommandGroup rootGroup = CommandGroup.COMMON;
        if (StringUtils.isNotEmpty(query)) {
            rootGroup = CommandGroup.getForTitle(query);
        }
        if (rootGroup == null || !groupedCommands.containsKey(rootGroup)) {
            messageService.onError(message.getChannel(), "Указанной группы не существует");
            return false;
        }

        List<EmbedBuilder> messages = new ArrayList<>();
        EmbedBuilder embedBuilder = getBaseEmbed(rootGroup, message);
        messages.add(embedBuilder);

        groupedCommands.remove(rootGroup).forEach(e -> embedBuilder.addField(context.getPrefix() + e.key(), e.description(), false));
        if (CommandGroup.COMMON.equals(rootGroup)) {
            groupedCommands.forEach((group, commands) -> {
                if (direct) {
                    EmbedBuilder groupBuilder = getBaseEmbed(group, message);
                    commands.forEach(e -> groupBuilder.addField(context.getPrefix() + e.key(), e.description(), false));
                    messages.add(groupBuilder);
                } else {
                    embedBuilder.addField(String.format("%s (%sхелп %s):", group.getTitle(), context.getPrefix(), group.getTitle().toLowerCase()),
                            commands.stream().map(e -> '`' + context.getPrefix() + e.key() + '`').collect(Collectors.joining(", ")), false);
                }
            });
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
            message.getChannel().sendMessage(String.format("%s я отправила список команд тебе в личку :fox:", message.getAuthor().getAsMention())).queue();
        }
        return true;
    }

    private EmbedBuilder getBaseEmbed(CommandGroup group, MessageReceivedEvent message) {
        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setThumbnail(message.getJDA().getSelfUser().getAvatarUrl())
                .setColor(discordConfig.getAccentColor())
                .setDescription(String.format("**Доступные команды%s:**", CommandGroup.COMMON.equals(group)
                        ? "" : String.format(" группы \"%s\"", group.getTitle())));
        if (StringUtils.isNotEmpty(discordConfig.getCopyContent())) {
            embedBuilder.setFooter(discordConfig.getCopyContent(), discordConfig.getCopyImageUrl());
        }
        return embedBuilder;
    }
}
