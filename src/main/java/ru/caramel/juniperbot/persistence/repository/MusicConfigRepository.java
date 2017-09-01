package ru.caramel.juniperbot.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.caramel.juniperbot.persistence.entity.MusicConfig;

@Repository
public interface MusicConfigRepository extends JpaRepository<MusicConfig, Long> {
}
