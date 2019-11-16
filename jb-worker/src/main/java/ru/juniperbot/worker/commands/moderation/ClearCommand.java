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
package ru.juniperbot.worker.commands.moderation;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import ru.juniperbot.common.model.AuditActionType;
import ru.juniperbot.common.model.exception.DiscordException;
import ru.juniperbot.common.utils.CommonUtils;
import ru.juniperbot.common.worker.command.model.BotContext;
import ru.juniperbot.common.worker.command.model.DiscordCommand;
import ru.juniperbot.common.worker.command.model.MemberReference;
import ru.juniperbot.common.worker.modules.audit.model.AuditActionBuilder;
import ru.juniperbot.common.worker.modules.audit.service.ActionsHolderService;
import ru.juniperbot.common.worker.modules.audit.service.AuditService;
import ru.juniperbot.common.worker.utils.DiscordUtils;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import static ru.juniperbot.common.worker.modules.audit.provider.MessagesClearAuditForwardProvider.MESSAGE_COUNT;

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
public class ClearCommand extends MentionableModeratorCommand {

    private static final int MAX_MESSAGES = 1000;

    private static final Pattern COUNT_PATTERN = Pattern.compile("^(\\d+)");

    @Autowired
    private ActionsHolderService actionsHolderService;

    @Autowired
    private AuditService auditService;

    public ClearCommand() {
        super(true, false);
    }

    @Override
    protected boolean doCommand(MemberReference reference, GuildMessageReceivedEvent event, BotContext context, String query) throws DiscordException {
        Member member = reference.getMember();
        String moderatorId = event.getMember().getId();
        String executionMessageId = event.getMessageId();

        boolean includeInvocation = reference.isAuthorSelected() || (member != null && member.equals(event.getMember()));

        if (StringUtils.isBlank(query) || !StringUtils.isNumeric(query)) {
            showHelp(event, context);
            return false;
        }
        int number = Integer.parseInt(query);
        if (number == 0 || number > MAX_MESSAGES) {
            showHelp(event, context);
            return false;
        }
        final int finalNumber = number + (includeInvocation ? 1 : 0);

        String userId = reference.isAuthorSelected() ? null : reference.getId();

        DateTime limit = new DateTime()
                .minusWeeks(2)
                .minusHours(1);

        TextChannel channel = event.getChannel();

        boolean canAttach = channel.getGuild().getSelfMember().hasPermission(channel, Permission.MESSAGE_ATTACH_FILES);
        StringBuilder history = new StringBuilder();
        DateTimeFormatter formatter = DateTimeFormat.mediumDateTime()
                .withLocale(contextService.getLocale())
                .withZone(context.getTimeZone());

        Set<String> ids = ConcurrentHashMap.newKeySet();

        channel.sendTyping().queue(r -> {
            channel.getIterableHistory()
                    .takeWhileAsync(e -> {
                        if (CommonUtils.getDate(e.getTimeCreated()).isBefore(limit)) {
                            return false;
                        }
                        if (StringUtils.isEmpty(userId)
                                || e.getMember() != null && Objects.equals(e.getMember().getId(), userId)) {
                            ids.add(e.getId());
                            if (canAttach && !executionMessageId.equals(e.getId())) {
                                appendHistory(history, formatter, e);
                            }
                        }
                        return ids.size() < finalNumber;
                    })
                    .exceptionally(e -> {
                        log.error("Clear failed", e);
                        fail(event);
                        return null;
                    })
                    .whenCompleteAsync((messages, t) -> contextService.withContext(context.getConfig().getGuildId(), () -> {
                        int finalCount = ids.size();
                        if (includeInvocation) {
                            finalCount--;
                        }
                        if (finalCount <= 0) {
                            messageService.onEmbedMessage(event.getChannel(), "discord.command.mod.clear.absent");
                            return;
                        }

                        ids.forEach(e -> actionsHolderService.markAsDeleted(channel.getId(), e));
                        channel.purgeMessagesById(new ArrayList<>(ids));

                        AuditActionBuilder builder = auditService.log(channel.getGuild(), AuditActionType.MESSAGES_CLEAR)
                                .withChannel(channel)
                                .withUser(channel.getGuild().getMemberById(moderatorId))
                                .withAttribute(MESSAGE_COUNT, finalCount);
                        if (canAttach && history.length() > 0) {
                            builder.withAttachment(channel.getId() + ".log", history.toString().getBytes());
                        }
                        builder.save();

                        String pluralMessages = messageService.getCountPlural(finalCount, "discord.plurals.message");
                        messageService.onTempMessage(channel, 5, "discord.command.mod.clear.deleted", finalCount, pluralMessages);
                    }));
        });
        return true;
    }

    @Override
    protected void showHelp(GuildMessageReceivedEvent event, BotContext context) {
        String clearCommand = messageService.getMessageByLocale("discord.command.mod.clear.key",
                context.getCommandLocale());
        messageService.onEmbedMessage(event.getChannel(), "discord.command.mod.clear.help",
                context.getConfig().getPrefix(), clearCommand);
    }

    private void appendHistory(StringBuilder history, DateTimeFormatter formatter, Message message) {
        if (history.length() > 0) {
            history.append("\n\n");
        }

        Member member = message.getMember();
        User user = message.getAuthor();

        history.append("[")
                .append(formatter.print(CommonUtils.getDate(message.getTimeCreated())))
                .append("] ");
        if (member == null || member.getNickname() == null) {
            history.append(message.getAuthor().getAsTag());
        } else {
            history.append(member.getEffectiveName())
                    .append(" (")
                    .append(member.getUser().getAsTag())
                    .append(")");
        }
        if (user.isBot()) {
            history.append(" [BOT]");
        }
        history.append(":");

        String content = DiscordUtils.getContent(message);
        if (StringUtils.isNotEmpty(content)) {
            history.append("\n").append(content);
        }

        int i = 0;
        for (MessageEmbed embed : message.getEmbeds()) {
            history.append("\n[embed")
                    .append(++i)
                    .append("]");
            if (StringUtils.isNotEmpty(embed.getDescription())) {
                history.append("\n").append(embed.getDescription());
            }
        }
    }
}
