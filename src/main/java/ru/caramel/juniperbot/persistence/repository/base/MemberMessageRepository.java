package ru.caramel.juniperbot.persistence.repository.base;

import java.util.List;

import org.springframework.data.repository.NoRepositoryBean;
import ru.caramel.juniperbot.persistence.entity.base.MemberMessageEntity;

@NoRepositoryBean
public interface MemberMessageRepository<T extends MemberMessageEntity> extends MemberRepository<T> {

    List<T> findByGuildIdAndChannelIdAndUserId(String guildId, String channelId, String userId);
}
