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
package ru.caramel.juniperbot.module.audio.commands;

import com.google.api.services.youtube.model.Video;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import ru.juniperbot.common.service.YouTubeService;
import ru.juniperbot.worker.common.command.model.BotContext;
import ru.juniperbot.worker.common.command.model.DiscordCommand;
import ru.juniperbot.worker.common.event.listeners.ReactionsListener;
import ru.juniperbot.common.utils.CommonUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@DiscordCommand(
        key = "discord.command.youtube.key",
        description = "discord.command.youtube.desc",
        group = "discord.command.group.music",
        priority = 103)
public class YouTubeCommand extends PlayCommand {

    @Autowired
    private YouTubeService youTubeService;

    @Autowired
    private ReactionsListener reactionsListener;

    @Override
    public boolean doInternal(GuildMessageReceivedEvent message, BotContext context, String content) {
        contextService.withContextAsync(message.getGuild(), () -> {
            message.getChannel().sendTyping().queue();
            List<Video> results = youTubeService.searchDetailed(content, 10L);
            if (results.isEmpty()) {
                messageManager.onNoMatches(message.getChannel(), content);
                return;
            }

            EmbedBuilder builder = messageService.getBaseEmbed();
            builder.setTitle(messageService.getMessage("discord.command.audio.search.results"));

            List<String> urls = new ArrayList<>();
            for (int i = 0; i < results.size(); i++) {
                Video result = results.get(i);
                long duration = Duration.parse(result.getContentDetails().getDuration()).toMillis();
                String url = youTubeService.getUrl(result);
                String title = String.format("%d. `[%s]` [%s](%s)", i + 1, CommonUtils.formatDuration(duration), result.getSnippet().getTitle(),
                        url);
                builder.addField(EmbedBuilder.ZERO_WIDTH_SPACE, title, false);
                urls.add(url);
            }

            String playCommand = messageService.getMessageByLocale("discord.command.play.key", context.getCommandLocale());

            builder.addField(EmbedBuilder.ZERO_WIDTH_SPACE, messageService.getMessage("discord.command.audio.search.select",
                    context.getConfig().getPrefix(), playCommand), false);

            message.getChannel().sendMessage(builder.build()).queue(e -> {
                List<CompletableFuture<Void>> actions = new ArrayList<>(10);
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
                reactionsListener.onReactionAdd(e.getId(), event -> {
                    if (!event.getUser().equals(event.getJDA().getSelfUser())) {
                        String emote = event.getReaction().getReactionEmote().getName();
                        int index = ArrayUtils.indexOf(ReactionsListener.CHOICES, emote);
                        if (index >= 0 && playerService.isInChannel(event.getMember())) {
                            String query = getChoiceUrl(context, index);
                            playerService.loadAndPlay(message.getChannel(), event.getMember(), query);
                            return true;
                        }
                    }
                    return false;
                });
            });
        });
        return true;
    }

    @Override
    protected boolean isChannelRestricted() {
        return false;
    }

    @Override
    protected boolean isActiveOnly() {
        return false;
    }
}
