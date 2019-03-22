package ru.caramel.juniperbot.module.wikifur.commands;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.sourceforge.jwbf.core.contentRep.SearchResult;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import ru.caramel.juniperbot.core.command.model.AbstractCommand;
import ru.caramel.juniperbot.core.command.model.BotContext;
import ru.caramel.juniperbot.core.command.model.DiscordCommand;
import ru.caramel.juniperbot.core.common.model.exception.DiscordException;
import ru.caramel.juniperbot.core.event.listeners.ReactionsListener;
import ru.caramel.juniperbot.module.wikifur.service.WikiFurService;

import java.util.List;

@DiscordCommand(
        key = "discord.command.wikifur.key",
        description = "discord.command.wikifur.desc",
        group = "discord.command.group.utility",
        priority = 10)
public class WikiFurCommand extends AbstractCommand {

    @Autowired
    private WikiFurService wikiFurService;

    @Autowired
    private ReactionsListener reactionsListener;

    @Override
    public boolean doCommand(MessageReceivedEvent message, BotContext context, String content) throws DiscordException {
        message.getChannel().sendTyping().queue();
        MessageEmbed embed = wikiFurService.renderArticle(content);
        if (embed != null) {
            messageService.sendMessageSilent(message.getChannel()::sendMessage, embed);
            return true;
        }

        List<SearchResult> searchResult = wikiFurService.search(content);
        if (searchResult.isEmpty()) {
            messageService.onError(message.getChannel(), "discord.command.wikifur.title", "discord.command.wikifur.noResults");
            return false;
        }
        // для единственного результата просто вернем его
        if (searchResult.size() == 1) {
            embed = wikiFurService.renderArticle(searchResult.get(0).getTitle());
            if (embed != null) {
                messageService.sendMessageSilent(message.getChannel()::sendMessage, embed);
                return true;
            } else {
                messageService.onError(message.getChannel(), "discord.command.wikifur.title", "discord.command.wikifur.noResults");
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
            String url = wikiFurService.getUrl(result.getTitle());
            String title = String.format("%d. [%s](%s)", i + 1, result.getTitle(), url);
            builder.addField(EmbedBuilder.ZERO_WIDTH_SPACE, title, false);
        }

        message.getChannel().sendMessage(builder.build()).queue(e -> {
            try {
                for (int i = 0; i < finalResult.size(); i++) {
                    e.addReaction(ReactionsListener.CHOICES[i]).queue();
                }
            } catch (Exception ex) {
                // ignore
            }
            reactionsListener.onReactionAdd(e.getId(), event -> {
                if (!event.getUser().equals(event.getJDA().getSelfUser())) {
                    String emote = event.getReaction().getReactionEmote().getName();
                    int index = ArrayUtils.indexOf(ReactionsListener.CHOICES, emote);
                    if (index >= 0 && index < finalResult.size() && message.getAuthor().equals(event.getUser())) {
                        messageService.delete(e);
                        SearchResult result = finalResult.get(index);
                        message.getTextChannel().sendTyping().queue();
                        MessageEmbed searchEmbed = wikiFurService.renderArticle(result.getTitle());
                        contextService.withContext(event.getGuild(), () -> {
                            if (searchEmbed == null) {
                                messageService.onError(message.getChannel(),
                                        "discord.command.wikifur.title",
                                        "discord.command.wikifur.noResults");
                                return;
                            }
                            messageService.sendMessageSilent(message.getChannel()::sendMessage, searchEmbed);
                        });
                        return true;
                    }
                }
                return false;
            });
        });
        return true;
    }
}
