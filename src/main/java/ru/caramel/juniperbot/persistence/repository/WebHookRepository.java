package ru.caramel.juniperbot.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.caramel.juniperbot.model.WebHookType;
import ru.caramel.juniperbot.persistence.entity.WebHook;

import java.util.List;

@Repository
public interface WebHookRepository extends JpaRepository<WebHook, Long> {

    @Query("SELECT w FROM WebHook w WHERE w.enabled = true AND w.hookId IS NOT NULL AND w.token IS NOT NULL AND w.type = :type")
    List<WebHook> findActive(@Param("type") WebHookType type);
}
