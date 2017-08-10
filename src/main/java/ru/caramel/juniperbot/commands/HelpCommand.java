package ru.caramel.juniperbot.commands;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import ru.caramel.juniperbot.configuration.DiscordConfig;
import ru.caramel.juniperbot.integration.discord.DiscordClient;
import ru.caramel.juniperbot.model.BotContext;

import java.util.Map;

@DiscordCommand(key = "хелп", description = "Отображает эту справку")
public class HelpCommand implements Command {

    @Autowired
    private DiscordConfig discordConfig;

    @Autowired
    private DiscordClient discordClient;

    @Override
    public boolean doCommand(MessageReceivedEvent message, BotContext context) {
        Map<String, Command> commands = discordClient.getCommands();
        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setThumbnail(message.getJDA().getSelfUser().getAvatarUrl());
        embedBuilder.setColor(discordConfig.getAccentColor());
        commands.forEach((k, v) -> {
            DiscordCommand annotation = v.getClass().getAnnotation(DiscordCommand.class);
            if (!annotation.hidden() && v.isApplicable(message.getChannel())) {
                embedBuilder.addField(discordConfig.getPrefix() + k, annotation.description(), false);
            }
        });
        if (StringUtils.isNotEmpty(discordConfig.getCopyContent())) {
            embedBuilder.setFooter(discordConfig.getCopyContent(), discordConfig.getCopyImageUrl());
        }
        message.getChannel().sendMessage(new MessageBuilder()
                .append("**Доступные команды:**")
                .setEmbed(embedBuilder.build()).build()).queue();
        return true;
    }
}
