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
package ru.caramel.juniperbot.core.audit.service;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.caramel.juniperbot.core.configuration.SchedulerConfiguration;
import ru.caramel.juniperbot.core.audit.model.AuditActionBuilder;
import ru.caramel.juniperbot.core.audit.model.AuditActionType;
import ru.caramel.juniperbot.core.audit.persistence.AuditConfig;
import ru.caramel.juniperbot.core.common.persistence.LocalMember;
import ru.caramel.juniperbot.core.audit.persistence.MessageHistory;
import ru.caramel.juniperbot.core.audit.persistence.MessageHistoryRepository;
import ru.caramel.juniperbot.core.common.service.MemberService;

import java.util.Date;
import java.util.Objects;
import java.util.stream.Collectors;

import static ru.caramel.juniperbot.core.audit.provider.MessageEditAuditForwardProvider.*;

@Service
public class HistoryServiceImpl implements HistoryService {

    @Autowired
    private AuditService auditService;

    @Autowired
    private MemberService memberService;

    @Autowired
    @Qualifier(SchedulerConfiguration.COMMON_SCHEDULER_NAME)
    protected TaskScheduler scheduler;

    @Autowired
    private MessageHistoryRepository historyRepository;

    @Value("${discord.history.enabled:true}")
    private boolean historyEnabled;

    @Value("${discord.history.durationDays:7}")
    private int durationDays;

    @Override
    @Transactional
    public void onMessageCreate(Message message) {
        if (!isEnabled(message.getGuild())) {
            return;
        }
        MessageHistory messageHistory = createHistory(message);
        historyRepository.save(messageHistory);
    }

    @Override
    @Transactional
    public void onMessageUpdate(Message message) {
        if (!isEnabled(message.getGuild())) {
            return;
        }
        MessageHistory history = historyRepository.findByChannelIdAndMessageId(message.getTextChannel().getId(), message.getId());
        if (history == null) {
            history = createHistory(message);
            historyRepository.save(history);
            return;
        }
        String oldContent = history.getMessage();
        String newContent = getContent(message);
        if (Objects.equals(oldContent, newContent)) {
            return;
        }
        history.setMessage(newContent);
        history.setUpdateDate(new Date());
        historyRepository.save(history);

        auditService.log(message.getGuild(), AuditActionType.MESSAGE_EDIT)
                .withUser(message.getMember())
                .withChannel(message.getTextChannel())
                .withAttribute(MESSAGE_ID, message.getId())
                .withAttribute(OLD_CONTENT, oldContent)
                .withAttribute(NEW_CONTENT, newContent)
                .save();
    }

    @Override
    @Transactional
    public void onMessageDelete(TextChannel channel, String messageId) {
        if (!isEnabled(channel.getGuild())) {
            return;
        }
        MessageHistory history = historyRepository.findByChannelIdAndMessageId(channel.getId(), messageId);
        if (history == null) {
            return;
        }
        historyRepository.delete(history);

        AuditActionBuilder builder = auditService.log(channel.getGuild(), AuditActionType.MESSAGE_DELETE)
                .withChannel(channel)
                .withAttribute(MESSAGE_ID, messageId)
                .withAttribute(OLD_CONTENT, history.getMessage());

        Member member = channel.getGuild().getMemberById(history.getUserId());
        if (member != null) {
            builder.withUser(member);
        } else {
            LocalMember localMember = memberService.get(history.getGuildId(), history.getUserId());
            if (localMember != null) {
                builder.withUser(localMember);
            }
        }
        builder.save();
    }

    @Scheduled(cron="0 0 0 * * ?")
    @Transactional
    public void runCleanUp() {
        runCleanUp(this.durationDays);
    }

    @Override
    @Transactional
    public void runCleanUp(int durationDays) {
        historyRepository.deleteByCreateDateBefore(DateTime.now().minusDays(durationDays).toDate());
    }

    private boolean isEnabled(Guild guild) {
        if (!historyEnabled) {
            return false;
        }
        AuditConfig config = auditService.get(guild);
        return config != null && config.isEnabled();
    }

    private static MessageHistory createHistory(Message message) {
        MessageHistory messageHistory = new MessageHistory();
        messageHistory.setGuildId(message.getGuild().getIdLong());
        messageHistory.setUserId(message.getAuthor().getId());
        messageHistory.setChannelId(message.getTextChannel().getId());
        messageHistory.setMessage(getContent(message));
        messageHistory.setMessageId(message.getId());
        messageHistory.setCreateDate(new Date());
        messageHistory.setUpdateDate(messageHistory.getCreateDate());
        return messageHistory;
    }

    private static String getContent(Message message) {
        StringBuilder builder = new StringBuilder(message
                .getContentStripped()
                .replaceAll("\u0000", ""));
        String attachmentsPart = message.getAttachments().stream()
                .map(Message.Attachment::getUrl)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(",\n"));
        if (StringUtils.isNotEmpty(attachmentsPart)) {
            if (builder.length() > 0) {
                builder.append("\n");
            }
            builder.append("---");
            builder.append(attachmentsPart);
        }
        return builder.toString();
    }
}
