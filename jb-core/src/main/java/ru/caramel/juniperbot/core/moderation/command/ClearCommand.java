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
package ru.caramel.juniperbot.core.moderation.command;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import ru.caramel.juniperbot.core.audit.service.ActionsHolderService;
import ru.caramel.juniperbot.core.command.model.BotContext;
import ru.caramel.juniperbot.core.command.model.DiscordCommand;
import ru.caramel.juniperbot.core.common.model.exception.DiscordException;
import ru.caramel.juniperbot.core.common.model.exception.ValidationException;
import ru.caramel.juniperbot.core.utils.CommonUtils;

import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@DiscordCommand(key = "discord.command.mod.clear.key",
        description = "discord.command.mod.clear.desc",
        group = "discord.command.group.moderation",
        permissions = {
                Permission.MESSAGE_WRITE,
                Permission.MESSAGE_EMBED_LINKS,
                Permission.MESSAGE_MANAGE,
                Permission.MESSAGE_HISTORY
        }
)
public class ClearCommand extends ModeratorCommandAsync {

    private static final int MAX_MESSAGES = 1000;

    private static final Pattern COUNT_PATTERN = Pattern.compile("^(\\d+)");

    @Autowired
    private ActionsHolderService actionsHolderService;

    @Override
    protected void doCommandAsync(GuildMessageReceivedEvent event, BotContext context, String query) throws DiscordException {
        TextChannel channel = event.getChannel();

        int number = getCount(query);
        Member mentioned = getMentioned(event);

        boolean includeInvocation = mentioned == null || mentioned.equals(event.getMember());
        if (includeInvocation) {
            number = number + 1; // delete command invocation too
        }
        final int finalNumber = number;

        DateTime limit = new DateTime()
                .minusWeeks(2)
                .minusHours(1);

        channel.sendTyping().queue(r -> channel.getIterableHistory()
                .takeAsync(finalNumber)
                .thenApplyAsync(e -> {
                    Stream<Message> stream = e.stream()
                            .filter(m -> CommonUtils.getDate(m.getTimeCreated()).isAfter(limit));
                    if (mentioned != null) {
                        stream = stream.filter(m -> Objects.equals(m.getMember(), mentioned));
                    }
                    List<Message> messageList = stream.collect(Collectors.toList());
                    messageList.forEach(actionsHolderService::markAsDeleted);
                    channel.purgeMessages(messageList);
                    return messageList.size();
                }).exceptionally(e -> {
                    log.error("Clear failed", e);
                    fail(event);
                    return null;
                }).whenCompleteAsync((count, e) -> contextService.withContext(context.getConfig().getGuildId(), () -> {
                    int finalCount = count;
                    if (includeInvocation) {
                        finalCount--;
                    }
                    if (finalCount <= 0) {
                        messageService.onEmbedMessage(event.getChannel(), "discord.mod.clear.absent");
                        return;
                    }
                    String pluralMessages = messageService.getCountPlural(finalCount, "discord.plurals.message");
                    messageService.onTempMessage(channel, 5, "discord.mod.clear.deleted", finalCount, pluralMessages);
                })));
    }

    private int getCount(String queue) throws DiscordException {
        int result = 10;
        if (StringUtils.isNotBlank(queue)) {
            Matcher matcher = COUNT_PATTERN.matcher(queue.trim());
            if (!matcher.find()) {
                throw new ValidationException("discord.mode.clear.help");
            }
            try {
                result = Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                result = MAX_MESSAGES;
            }
        }
        return Math.min(result, MAX_MESSAGES);
    }
}
