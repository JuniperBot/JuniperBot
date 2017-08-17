package ru.caramel.juniperbot.commands.common;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import ru.caramel.juniperbot.audio.service.MessageManager;
import ru.caramel.juniperbot.commands.base.AbstractCommand;
import ru.caramel.juniperbot.commands.base.Command;
import ru.caramel.juniperbot.commands.model.CommandGroup;
import ru.caramel.juniperbot.commands.model.DiscordCommand;
import ru.caramel.juniperbot.configuration.DiscordConfig;
import ru.caramel.juniperbot.integration.discord.DiscordClient;
import ru.caramel.juniperbot.commands.model.BotContext;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@DiscordCommand(key = "хелп", description = "Отображает эту справку")
public class HelpCommand extends AbstractCommand {

    @Autowired
    private DiscordConfig discordConfig;

    @Autowired
    private DiscordClient discordClient;

    @Autowired
    private MessageManager messageManager;

    @Override
    public boolean doCommand(MessageReceivedEvent message, BotContext context, String query) {
        CommandGroup rootGroup = CommandGroup.COMMON;
        boolean common = true;
        if (StringUtils.isNotEmpty(query)) {
            rootGroup = CommandGroup.getForTitle(query);
            common = false;
        }
        if (rootGroup == null) {
            messageManager.onError(message.getChannel(), "Указанной группы не существует");
            return false;
        }

        List<DiscordCommand> discordCommands = discordClient.getCommands().entrySet().stream()
                .filter(e -> e.getValue().isApplicable(message.getChannel()))
                .map(e -> e.getValue().getClass().getAnnotation(DiscordCommand.class))
                .filter(e -> !e.hidden())
                .collect(Collectors.toList());

        Map<CommandGroup, List<DiscordCommand>> groupedCommands = discordCommands
                .stream().collect(Collectors.groupingBy(DiscordCommand::group));
        groupedCommands.forEach((k, v) -> v.sort(Comparator.comparing(DiscordCommand::key)));

        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setThumbnail(message.getJDA().getSelfUser().getAvatarUrl())
                .setColor(discordConfig.getAccentColor())
                .setDescription(String.format("**Доступные команды%s:**", common ? "" : String.format(" группы \"%s\"", rootGroup.getTitle())));

        if (!groupedCommands.containsKey(rootGroup)) {
            messageManager.onError(message.getChannel(), "Указанной группы не существует");
            return false;
        }

        groupedCommands.remove(rootGroup).forEach(e -> embedBuilder.addField(context.getPrefix() + e.key(), e.description(), false));
        if (common) {
            groupedCommands.forEach((group, commands) -> {
                embedBuilder.addField(String.format("%s (%sхелп %s):", group.getTitle(), context.getPrefix(), group.getTitle().toLowerCase()),
                        commands.stream().map(e -> '`' + context.getPrefix() + e.key() + '`').collect(Collectors.joining(", ")), false);
            });
        }

        if (StringUtils.isNotEmpty(discordConfig.getCopyContent())) {
            embedBuilder.setFooter(discordConfig.getCopyContent(), discordConfig.getCopyImageUrl());
        }
        message.getChannel().sendMessage(embedBuilder.build()).queue();
        return true;
    }
}
