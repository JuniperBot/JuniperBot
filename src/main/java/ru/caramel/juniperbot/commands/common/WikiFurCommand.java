package ru.caramel.juniperbot.commands.common;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.sourceforge.jwbf.core.contentRep.SearchResult;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import ru.caramel.juniperbot.commands.Command;
import ru.caramel.juniperbot.commands.model.BotContext;
import ru.caramel.juniperbot.commands.model.CommandGroup;
import ru.caramel.juniperbot.commands.model.DiscordCommand;
import ru.caramel.juniperbot.integration.discord.model.DiscordException;
import ru.caramel.juniperbot.integration.wiki.WikiFurClient;
import ru.caramel.juniperbot.service.MessageService;
import ru.caramel.juniperbot.service.listeners.ReactionsListener;

import java.util.List;

@DiscordCommand(
        key = "discord.command.wikifur.key",
        description = "discord.command.wikifur.desc",
        group = CommandGroup.COMMON,
        priority = 3)
public class WikiFurCommand implements Command {

    @Autowired
    private WikiFurClient wikiFurClient;

    @Autowired
    private MessageService messageService;

    @Autowired
    private ReactionsListener reactionsListener;

    @Override
    public boolean doCommand(MessageReceivedEvent message, BotContext context, String content) throws DiscordException {
        message.getTextChannel().sendTyping().queue();
        MessageEmbed embed = wikiFurClient.renderArticle(content);
        if (embed != null) {
            messageService.sendMessageSilent(message.getChannel()::sendMessage, embed);
            return true;
        }

        List<SearchResult> searchResult = wikiFurClient.search(content);
        if (searchResult.isEmpty()) {
            messageService.onTitledMessage(message.getChannel(), "discord.command.wikifur.title", "discord.command.wikifur.noResults");
            return false;
        }
        // для единственного результата просто вернем его
        if (searchResult.size() == 1) {
            embed = wikiFurClient.renderArticle(searchResult.get(0).getTitle());
            if (embed != null) {
                messageService.sendMessageSilent(message.getChannel()::sendMessage, embed);
                return true;
            } else {
                messageService.onTitledMessage(message.getChannel(), "discord.command.wikifur.title", "discord.command.wikifur.noResults");
                return false;
            }
        }
        if (searchResult.size() > 10) {
            searchResult = searchResult.subList(0, 9);
        }
        final List<SearchResult> finalResult = searchResult;

        EmbedBuilder builder = messageService.getBaseEmbed();
        builder.setTitle(messageService.getMessage("discord.command.wikifur.search.results"));
        for (int i = 0; i < finalResult.size(); i++) {
            SearchResult result = finalResult.get(i);
            String url = wikiFurClient.getUrl(result.getTitle());
            String title = String.format("%d. [%s](%s)", i + 1, result.getTitle(), url);
            builder.addField(EmbedBuilder.ZERO_WIDTH_SPACE, title, false);
        }

        message.getChannel().sendMessage(builder.build()).queue(e -> {
            try {
                for (int i = 0; i < finalResult.size(); i++) {
                    e.addReaction(ReactionsListener.CHOICES[i]).submit();
                }
            } catch (Exception ex) {
                // ignore
            }
            reactionsListener.onReaction(e.getId(), event -> {
                if (!event.getUser().equals(event.getJDA().getSelfUser())) {
                    String emote = event.getReaction().getEmote().getName();
                    int index = ArrayUtils.indexOf(ReactionsListener.CHOICES, emote);
                    if (index >= 0 && index < finalResult.size() && message.getAuthor().equals(event.getUser())) {
                        e.delete().queue();
                        SearchResult result = finalResult.get(index);
                        message.getTextChannel().sendTyping().queue();
                        MessageEmbed searchEmbed = wikiFurClient.renderArticle(result.getTitle());
                        if (searchEmbed == null) {
                            messageService.onTitledMessage(message.getChannel(), "discord.command.wikifur.title", "discord.command.wikifur.noResults");
                            return true;
                        }
                        messageService.sendMessageSilent(message.getChannel()::sendMessage, searchEmbed);
                        return true;
                    }
                }
                return false;
            });
        });
        return true;
    }
}
