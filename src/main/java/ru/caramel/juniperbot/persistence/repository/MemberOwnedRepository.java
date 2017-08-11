package ru.caramel.juniperbot.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;
import ru.caramel.juniperbot.persistence.entity.MemberOwnedEntity;

@NoRepositoryBean
public interface MemberOwnedRepository<T extends MemberOwnedEntity> extends JpaRepository<T, Long> {

    List<T> findByGuildIdAndUserId(String guildId, String userId);
}
