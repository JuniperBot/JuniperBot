package ru.caramel.juniperbot.persistence.repository.base;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;
import ru.caramel.juniperbot.persistence.entity.base.GuildEntity;

import java.util.List;

@NoRepositoryBean
public interface GuildRepository<T extends GuildEntity> extends JpaRepository<T, Long> {

    List<T> findByGuildId(String guildId);
}
