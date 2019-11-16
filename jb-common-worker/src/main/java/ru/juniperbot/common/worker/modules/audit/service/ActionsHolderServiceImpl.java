package ru.juniperbot.common.worker.modules.audit.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.dv8tion.jda.api.entities.Message;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class ActionsHolderServiceImpl implements ActionsHolderService {

    private Cache<String, Boolean> leaveNotified = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.SECONDS)
            .build();

    private Cache<String, Boolean> messageDeleted = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();

    @Override
    public boolean isLeaveNotified(long guildId, long userId) {
        return Boolean.TRUE.equals(leaveNotified.getIfPresent(getMemberKey(guildId, userId)));
    }

    @Override
    public void setLeaveNotified(long guildId, long userId) {
        leaveNotified.put(getMemberKey(guildId, userId), true);
    }

    @Override
    public void markAsDeleted(Message message) {
        markAsDeleted(message.getChannel().getId(), message.getId());
    }

    @Override
    public void markAsDeleted(String channelId, String messageId) {
        messageDeleted.cleanUp();
        messageDeleted.put(geMessageKey(channelId, messageId), true);
    }

    @Override
    public boolean isOwnDeleted(String channelId, String messageId) {
        return Boolean.TRUE.equals(messageDeleted.getIfPresent(geMessageKey(channelId, messageId)));
    }

    private static String getMemberKey(long guildId, long userId) {
        return String.format("%s_%s", guildId, userId);
    }

    private static String geMessageKey(String channelId, String messageId) {
        return String.format("%s_%s", channelId, messageId);
    }
}
