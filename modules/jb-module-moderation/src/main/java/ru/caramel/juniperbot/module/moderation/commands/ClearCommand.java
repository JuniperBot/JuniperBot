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
package ru.caramel.juniperbot.module.moderation.commands;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import ru.caramel.juniperbot.core.model.BotContext;
import ru.caramel.juniperbot.core.model.DiscordCommand;
import ru.caramel.juniperbot.core.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

@DiscordCommand(key = "discord.command.clear.key",
        description = "discord.command.clear.desc",
        group = "discord.command.group.moderation",
        source = ChannelType.TEXT,
        permissions = {Permission.MESSAGE_MANAGE, Permission.MESSAGE_HISTORY},
        priority = 1)
public class ClearCommand extends ModeratorCommand {

    private static final int MAX_MESSAGES = 1000;

    @Override
    public boolean doCommand(MessageReceivedEvent event, BotContext context, String query) {
        TextChannel channel = event.getTextChannel();
        List<Message> messages;
        DateTime limit = new DateTime()
                .minusWeeks(2)
                .minusHours(1);
        if (CollectionUtils.isNotEmpty(event.getMessage().getMentionedUsers())) {
            Member mentioned = event.getGuild().getMember(event.getMessage().getMentionedUsers().get(0));
            if (mentioned == null) {
                return false;
            }
            messages = getMessages(channel, null, m -> {
                DateTime creationDate = CommonUtils.getDate(m.getCreationTime());
                return creationDate.isAfter(limit) && Objects.equals(m.getMember(), mentioned);
            });
        } else {
            int result = 100;
            if (StringUtils.isNotBlank(query) && StringUtils.isNumeric(query)) {
                result = Integer.parseInt(query);
            }
            int number = Math.min(result, MAX_MESSAGES);
            messages = getMessages(channel, number + 1, m -> {
                DateTime creationDate = CommonUtils.getDate(m.getCreationTime());
                return creationDate.isAfter(limit);
            });
        }

        if (CollectionUtils.isEmpty(messages)) {
            messageService.onError(event.getChannel(), "discord.mod.clear.absent");
            return fail(event);
        }
        int deletedCount = messages.size() - 1;
        String pluralMessages = messageService.getCountPlural(deletedCount, "discord.plurals.message");
        deleteMessages(channel, messages);
        messageService.onTempMessage(channel, 5, "discord.mod.clear.deleted", deletedCount, pluralMessages);
        return true;
    }

    private void deleteMessages(TextChannel channel, List<Message> messages) {
        Iterator<Message> iterator = messages.iterator();
        List<Message> chunk = new ArrayList<>(100);
        while (iterator.hasNext()) {
            chunk.clear();
            while (chunk.size() < 100 && iterator.hasNext()) {
                chunk.add(iterator.next());
            }
            if (!chunk.isEmpty()) {
                channel.sendTyping().submit();
                if (chunk.size() == 1) {
                    chunk.get(0).delete().complete();
                } else {
                    channel.deleteMessages(chunk).complete();
                }
            }
        }
    }

    private List<Message> getMessages(TextChannel channel, Integer limitCount, Predicate<Message> predicate) {
        List<Message> messages = limitCount != null ? new ArrayList<>(limitCount) : new ArrayList<>();
        MessageHistory history = channel.getHistory();
        while (limitCount == null || limitCount > 0) {
            int count = limitCount == null || limitCount > 100 ? 100 : limitCount;
            List<Message> retrieved = history.retrievePast(count).complete();
            if (CollectionUtils.isEmpty(retrieved)) {
                break;
            }
            for (Message message : retrieved) {
                if (!predicate.test(message)) {
                    break;
                }
                messages.add(message);
                if (limitCount != null) {
                    limitCount--;
                }
            }
        }
        return messages;
    }
}
