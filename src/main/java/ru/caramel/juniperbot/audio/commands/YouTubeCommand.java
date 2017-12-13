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
package ru.caramel.juniperbot.audio.commands;

import com.google.api.services.youtube.model.Video;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.requests.RequestFuture;
import org.apache.commons.lang3.ArrayUtils;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import ru.caramel.juniperbot.model.BotContext;
import ru.caramel.juniperbot.model.enums.CommandGroup;
import ru.caramel.juniperbot.model.enums.CommandSource;
import ru.caramel.juniperbot.model.DiscordCommand;
import ru.caramel.juniperbot.integration.discord.model.DiscordException;
import ru.caramel.juniperbot.integration.youtube.YouTubeClient;
import ru.caramel.juniperbot.service.MessageService;
import ru.caramel.juniperbot.service.listeners.ReactionsListener;
import ru.caramel.juniperbot.utils.CommonUtils;

import java.time.Duration;
import java.util.*;

@DiscordCommand(
        key = "discord.command.youtube.key",
        description = "discord.command.youtube.desc",
        source = CommandSource.GUILD,
        group = CommandGroup.MUSIC,
        priority = 103)
public class YouTubeCommand extends PlayCommand {

    @Autowired
    private YouTubeClient youTubeClient;

    @Autowired
    private MessageService messageService;

    @Autowired
    private ReactionsListener reactionsListener;

    @Override
    public boolean doInternal(MessageReceivedEvent message, BotContext context, String content) throws DiscordException {
        message.getTextChannel().sendTyping().queue();
        List<Video> results = youTubeClient.searchDetailed(content, 10L);
        if (results.isEmpty()) {
            messageManager.onNoMatches(message.getChannel(), content);
            return false;
        }

        EmbedBuilder builder = messageService.getBaseEmbed();
        builder.setTitle(messageService.getMessage("discord.command.audio.search.results"));

        List<String> urls = new ArrayList<>();
        for (int i = 0; i < results.size(); i++) {
            Video result = results.get(i);
            long duration = Duration.parse(result.getContentDetails().getDuration()).toMillis();
            String url = youTubeClient.getUrl(result);
            String title = String.format("%d. `[%s]` [%s](%s)", i + 1, CommonUtils.formatDuration(duration), result.getSnippet().getTitle(),
                    url);
            builder.addField(EmbedBuilder.ZERO_WIDTH_SPACE, title, false);
            urls.add(url);
        }

        builder.addField(EmbedBuilder.ZERO_WIDTH_SPACE, messageService.getMessage("discord.command.audio.search.select",
                context.getPrefix()), false);

        Hibernate.initialize(context.getConfig().getMusicConfig());
        message.getChannel().sendMessage(builder.build()).queue(e -> {
            List<RequestFuture<Void>> actions = new ArrayList<>(10);
            try {
                for (int i = 0; i < results.size(); i++) {
                    actions.add(e.addReaction(ReactionsListener.CHOICES[i]).submit());
                }
            } catch (Exception ex) {
                // ignore
            }
            context.putAttribute(ATTR_SEARCH_RESULTS, urls);
            context.putAttribute(ATTR_SEARCH_MESSAGE, e);
            context.putAttribute(ATTR_SEARCH_ACTIONS, actions);
            reactionsListener.onReaction(e.getId(), event -> {
                if (!event.getUser().equals(event.getJDA().getSelfUser())) {
                    String emote = event.getReaction().getEmote().getName();
                    int index = ArrayUtils.indexOf(ReactionsListener.CHOICES, emote);
                    if (index >= 0 && playerService.isInChannel(event.getMember())) {
                        String query = getChoiceUrl(context, index);
                        loadAndPlay(message.getTextChannel(), context, event.getMember(), query);
                        return true;
                    }
                }
                return false;
            });
        });
        return true;
    }

    @Override
    protected boolean isChannelRestricted() {
        return false;
    }
}
