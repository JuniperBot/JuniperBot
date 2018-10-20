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

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.requests.RequestFuture;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import ru.caramel.juniperbot.core.model.BotContext;
import ru.caramel.juniperbot.core.model.DiscordCommand;
import ru.caramel.juniperbot.core.model.exception.DiscordException;

import java.util.List;

@DiscordCommand(
        key = PlayCommand.KEY,
        description = "discord.command.play.desc",
        group = "discord.command.group.music",
        source = ChannelType.TEXT,
        permissions = {
                Permission.MESSAGE_WRITE,
                Permission.MESSAGE_EMBED_LINKS,
                Permission.VOICE_CONNECT,
                Permission.VOICE_SPEAK
        },
        priority = 100)
public class PlayCommand extends AudioCommand {

    public static final String KEY = "discord.command.play.key";

    protected static final String ATTR_SEARCH_MESSAGE = "search-message";

    protected static final String ATTR_SEARCH_RESULTS = "search-results";

    protected static final String ATTR_SEARCH_ACTIONS = "search-actions";

    @SuppressWarnings("unchecked")
    @Override
    public boolean doInternal(MessageReceivedEvent message, BotContext context, String query) throws DiscordException {
        if (!message.getMessage().getAttachments().isEmpty()) {
            query = message.getMessage().getAttachments().get(0).getUrl();
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
        message.getTextChannel().sendTyping().queue();
        final String finalQuery = query;
        contextService.withContextAsync(message.getGuild(), () -> {
            playerService.loadAndPlay(message.getTextChannel(), message.getMember(), finalQuery);
        });
        return true;
    }

    @SuppressWarnings("unchecked")
    protected String getChoiceUrl(BotContext context, int index) {
        List<String> results = (List<String>) context.getAttribute(ATTR_SEARCH_RESULTS);
        if (index < 0 || CollectionUtils.isEmpty(results) || index > results.size() - 1) {
            return null;
        }
        List<RequestFuture<Void>> actions = (List<RequestFuture<Void>>) context.getAttribute(ATTR_SEARCH_ACTIONS);
        if (actions != null) {
            actions.forEach(e1 -> e1.cancel(true));
            context.removeAttribute(ATTR_SEARCH_ACTIONS);
        }
        context.removeAttribute(Message.class, ATTR_SEARCH_MESSAGE).delete().queue();
        return (String) context.removeAttribute(List.class, ATTR_SEARCH_RESULTS).get(index);
    }
}
