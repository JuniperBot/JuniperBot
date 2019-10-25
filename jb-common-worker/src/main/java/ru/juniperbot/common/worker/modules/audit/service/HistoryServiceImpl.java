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
package ru.juniperbot.common.worker.modules.audit.service;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.iv.StringFixedIvGenerator;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.juniperbot.common.model.AuditActionType;
import ru.juniperbot.common.persistence.entity.AuditConfig;
import ru.juniperbot.common.persistence.entity.LocalMember;
import ru.juniperbot.common.persistence.entity.MessageHistory;
import ru.juniperbot.common.persistence.repository.MessageHistoryRepository;
import ru.juniperbot.common.service.AuditConfigService;
import ru.juniperbot.common.service.MemberService;
import ru.juniperbot.common.worker.configuration.WorkerProperties;
import ru.juniperbot.common.worker.modules.audit.model.AuditActionBuilder;
import ru.juniperbot.common.worker.utils.DiscordUtils;

import java.util.Date;
import java.util.Objects;

import static ru.juniperbot.common.worker.modules.audit.provider.MessageEditAuditForwardProvider.*;

@Service
public class HistoryServiceImpl implements HistoryService {

    @Autowired
    private AuditService auditService;

    @Autowired
    private AuditConfigService auditConfigService;

    @Autowired
    private MemberService memberService;

    @Autowired
    private MessageHistoryRepository historyRepository;

    @Autowired
    private WorkerProperties workerProperties;

    @Override
    @Transactional
    public void onMessageCreate(Message message) {
        if (isDisabled(message.getGuild())) {
            return;
        }
        MessageHistory messageHistory = createHistory(message);
        historyRepository.save(messageHistory);
    }

    @Override
    @Transactional
    public void onMessageUpdate(Message message) {
        if (isDisabled(message.getGuild())) {
            return;
        }
        MessageHistory history = historyRepository.findByMessageId(message.getId());
        if (history == null) {
            history = createHistory(message);
            historyRepository.save(history);
            return;
        }
        TextChannel channel = message.getTextChannel();
        String oldContent = decrypt(channel, history);
        String newContent = DiscordUtils.getContent(message);
        if (Objects.equals(oldContent, newContent)) {
            return;
        }
        history.setMessage(encrypt(channel, message.getId(), newContent));
        history.setUpdateDate(new Date());
        historyRepository.save(history);

        auditService.log(message.getGuild(), AuditActionType.MESSAGE_EDIT)
                .withUser(message.getMember())
                .withChannel(channel)
                .withAttribute(MESSAGE_ID, message.getId())
                .withAttribute(OLD_CONTENT, oldContent)
                .withAttribute(NEW_CONTENT, newContent)
                .save();
    }

    @Override
    @Transactional
    public void onMessageDelete(TextChannel channel, String messageId) {
        Guild guild = channel.getGuild();
        if (isDisabled(guild)) {
            return;
        }
        MessageHistory history = historyRepository.findByMessageId(messageId);
        if (history == null) {
            return;
        }
        historyRepository.delete(history);

        AuditActionBuilder builder = auditService.log(guild, AuditActionType.MESSAGE_DELETE)
                .withChannel(channel)
                .withAttribute(MESSAGE_ID, messageId)
                .withAttribute(OLD_CONTENT, decrypt(channel, history));

        Member member = guild.getMemberById(history.getUserId());
        if (member != null) {
            builder.withUser(member);
        } else {
            LocalMember localMember = memberService.get(guild.getIdLong(), history.getUserId());
            if (localMember != null) {
                builder.withUser(localMember);
            }
        }
        builder.save();
    }

    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void runCleanUp() {
        runCleanUp(this.workerProperties.getAudit().getHistoryDays());
    }

    @Override
    @Transactional
    public void runCleanUp(int durationDays) {
        historyRepository.deleteByCreateDateBefore(DateTime.now().minusDays(durationDays).toDate());
    }

    private boolean isDisabled(Guild guild) {
        if (!workerProperties.getAudit().isHistoryEnabled()) {
            return true;
        }
        AuditConfig config = auditConfigService.get(guild);
        return config == null || !config.isEnabled();
    }

    private MessageHistory createHistory(Message message) {
        MessageHistory messageHistory = new MessageHistory();
        messageHistory.setUserId(message.getAuthor().getId());
        messageHistory.setMessage(encrypt(message.getTextChannel(), message.getId(), DiscordUtils.getContent(message)));
        messageHistory.setMessageId(message.getId());
        messageHistory.setCreateDate(new Date());
        messageHistory.setUpdateDate(messageHistory.getCreateDate());
        return messageHistory;
    }

    private String encrypt(TextChannel channel, String iv, String content) {
        return workerProperties.getAudit().isHistoryEncryption()
                ? getEncryptor(channel, iv).encrypt(content)
                : content;
    }

    private String decrypt(TextChannel channel, MessageHistory history) {
        return workerProperties.getAudit().isHistoryEncryption()
                ? getEncryptor(channel, history.getMessageId()).decrypt(history.getMessage())
                : history.getMessage();
    }

    private StringEncryptor getEncryptor(TextChannel channel, String iv) {
        StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
        encryptor.setAlgorithm("PBEWithHMACSHA512AndAES_256");
        encryptor.setPassword(String.format("%s:%s", channel.getGuild().getId(), channel.getId()));
        encryptor.setIvGenerator(new StringFixedIvGenerator(iv));
        return encryptor;
    }
}
