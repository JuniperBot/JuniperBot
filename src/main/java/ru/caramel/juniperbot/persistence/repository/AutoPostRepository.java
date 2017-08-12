package ru.caramel.juniperbot.persistence.repository;

import org.springframework.stereotype.Repository;
import ru.caramel.juniperbot.persistence.entity.AutoPost;
import ru.caramel.juniperbot.persistence.repository.base.TextChannelRepository;

@Repository
public interface AutoPostRepository extends TextChannelRepository<AutoPost> {

}
