package ru.caramel.juniperbot.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.caramel.juniperbot.persistence.entity.GuildConfig;

@Repository
public interface GuildConfigRepository extends JpaRepository<GuildConfig, Long> {

    GuildConfig findByGuildId(long guildId);

    boolean existsByGuildId(long guildId);
}
