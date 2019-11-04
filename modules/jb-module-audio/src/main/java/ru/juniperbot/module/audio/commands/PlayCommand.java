/*
 * This file is part of JuniperBot.
 *
 * JuniperBot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * JuniperBot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with JuniperBot. If not, see <http://www.gnu.org/licenses/>.
 */
package ru.juniperbot.module.audio.commands;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import ru.juniperbot.common.worker.command.model.BotContext;
import ru.juniperbot.common.worker.command.model.DiscordCommand;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@DiscordCommand(
        key = PlayCommand.KEY,
        description = "discord.command.play.desc",
        group = "discord.command.group.music",
        priority = 100)
public class PlayCommand extends AudioCommand {

    public static final String KEY = "discord.command.play.key";

    protected static final String ATTR_SEARCH_MESSAGE = "search-message";

    protected static final String ATTR_SEARCH_RESULTS = "search-results";

    protected static final String ATTR_SEARCH_ACTIONS = "search-actions";

    @SuppressWarnings("unchecked")
    @Override
    public boolean doInternal(GuildMessageReceivedEvent message, BotContext context, String query) {
        if (!message.getMessage().getAttachments().isEmpty()) {
            query = message.getMessage().getAttachments().get(0).getUrl();
        }

        if (StringUtils.isBlank(query)) {
            showHelp(message, context);
            return true;
        }

        List<String> results = (List<String>) context.getAttribute(ATTR_SEARCH_RESULTS);
        if (StringUtils.isNumeric(query) && CollectionUtils.isNotEmpty(results)) {
            int index = Integer.parseInt(query) - 1;
            query = getChoiceUrl(context, index);
            if (query == null) {
                messageManager.onQueueError(message.getChannel(), "discord.command.audio.play.select", results.size());
                return fail(message);
            }
        }
        message.getChannel().sendTyping().queue();
        final String finalQuery = query;
        contextService.withContextAsync(message.getGuild(), () -> {
            playerService.loadAndPlay(message.getChannel(), message.getMember(), finalQuery);
        });
        return true;
    }

    @SuppressWarnings("unchecked")
    protected String getChoiceUrl(BotContext context, int index) {
        List<String> results = (List<String>) context.getAttribute(ATTR_SEARCH_RESULTS);
        if (index < 0 || CollectionUtils.isEmpty(results) || index > results.size() - 1) {
            return null;
        }
        List<CompletableFuture<Void>> actions = (List<CompletableFuture<Void>>) context.getAttribute(ATTR_SEARCH_ACTIONS);
        if (actions != null) {
            actions.forEach(e1 -> e1.cancel(true));
            context.removeAttribute(ATTR_SEARCH_ACTIONS);
        }
        messageService.delete(context.removeAttribute(Message.class, ATTR_SEARCH_MESSAGE));
        return (String) context.removeAttribute(List.class, ATTR_SEARCH_RESULTS).get(index);
    }

    @Override
    protected boolean isActiveOnly() {
        return false;
    }

    protected void showHelp(GuildMessageReceivedEvent event, BotContext context) {
        String helpCommand = messageService.getMessageByLocale("discord.command.help.key",
                context.getCommandLocale());
        String musicGroup = messageService.getMessageByLocale("discord.command.group.music",
                context.getCommandLocale());
        messageService.onEmbedMessage(event.getChannel(), "discord.command.play.help",
                context.getConfig().getPrefix(), helpCommand, musicGroup);
    }
}
