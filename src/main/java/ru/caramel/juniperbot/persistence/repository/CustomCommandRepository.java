package ru.caramel.juniperbot.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.caramel.juniperbot.persistence.entity.CustomCommand;
import ru.caramel.juniperbot.persistence.entity.GuildConfig;

import java.util.List;

@Repository
public interface CustomCommandRepository extends JpaRepository<CustomCommand, Long> {
    CustomCommand findByKeyAndConfig(String key, GuildConfig config);

    List<CustomCommand> findByConfig(GuildConfig config);
}
