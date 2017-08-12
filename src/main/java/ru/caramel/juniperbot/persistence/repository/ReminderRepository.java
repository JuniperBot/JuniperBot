package ru.caramel.juniperbot.persistence.repository;

import org.springframework.stereotype.Repository;
import ru.caramel.juniperbot.persistence.entity.Reminder;
import ru.caramel.juniperbot.persistence.repository.base.MemberMessageRepository;

@Repository
public interface ReminderRepository extends MemberMessageRepository<Reminder> {
}
