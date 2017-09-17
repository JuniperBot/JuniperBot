package ru.caramel.juniperbot.commands.common;

import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.sourceforge.jwbf.core.contentRep.Article;
import org.springframework.beans.factory.annotation.Autowired;
import ru.caramel.juniperbot.commands.audio.AudioCommand;
import ru.caramel.juniperbot.commands.model.BotContext;
import ru.caramel.juniperbot.commands.model.CommandGroup;
import ru.caramel.juniperbot.commands.model.DiscordCommand;
import ru.caramel.juniperbot.integration.discord.model.DiscordException;
import ru.caramel.juniperbot.integration.wiki.WikiFurClient;
import ru.caramel.juniperbot.service.MessageService;

@DiscordCommand(
        key = "discord.command.wikifur.key",
        description = "discord.command.wikifur.desc",
        group = CommandGroup.COMMON,
        priority = 3)
public class WikiFurCommand extends AudioCommand {

    @Autowired
    private WikiFurClient wikiFurClient;

    @Autowired
    private MessageService messageService;

    @Override
    public boolean doInternal(MessageReceivedEvent message, BotContext context, String content) throws DiscordException {
        Article article = wikiFurClient.getArticle(content);
        MessageEmbed embed = wikiFurClient.renderArticle(article);
        if (embed == null) {
            return false;
        }
        messageService.sendMessageSilent(message.getChannel()::sendMessage, embed);
        return true;
    }

    private String renderText(String content) {
        return content;
    }
}
