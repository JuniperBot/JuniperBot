package ru.caramel.juniperbot.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.caramel.juniperbot.persistence.entity.VkConnection;

@Repository
public interface VkConnectionRepository extends JpaRepository<VkConnection, Long> {

    VkConnection findByToken(String token);
}
