package ru.caramel.juniperbot.persistence.repository.base;

import java.util.List;

import org.springframework.data.repository.NoRepositoryBean;
import ru.caramel.juniperbot.persistence.entity.base.MemberEntity;

@NoRepositoryBean
public interface MemberRepository<T extends MemberEntity> extends GuildRepository<T> {

    List<T> findByGuildIdAndUserId(String guildId, String userId);
}
