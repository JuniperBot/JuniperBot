package ru.caramel.juniperbot.service;

import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.MessageChannel;
import ru.caramel.juniperbot.persistence.entity.Reminder;

import java.util.Date;

public interface ReminderService {
    Reminder createReminder(MessageChannel channel, Member member, String message, Date date);
}
