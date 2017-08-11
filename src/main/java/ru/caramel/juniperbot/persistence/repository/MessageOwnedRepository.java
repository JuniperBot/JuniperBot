package ru.caramel.juniperbot.persistence.repository;

import java.util.List;

import org.springframework.data.repository.NoRepositoryBean;
import ru.caramel.juniperbot.persistence.entity.MessageOwnedEntity;

@NoRepositoryBean
public interface MessageOwnedRepository<T extends MessageOwnedEntity> extends MemberOwnedRepository<T> {

    List<T> findByGuildIdAndChannelIdAndUserId(String guildId, String channelId, String userId);
}
