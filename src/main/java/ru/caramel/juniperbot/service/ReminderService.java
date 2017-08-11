package ru.caramel.juniperbot.service;

import java.util.Date;

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.ReadyEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import ru.caramel.juniperbot.integration.discord.DiscordClient;
import ru.caramel.juniperbot.integration.discord.model.DiscordEvent;
import ru.caramel.juniperbot.persistence.entity.Reminder;
import ru.caramel.juniperbot.persistence.repository.ReminderRepository;

@Service
public class ReminderService implements ApplicationListener<DiscordEvent> {

    @Autowired
    private ReminderRepository reminderRepository;

    @Autowired
    private TaskScheduler taskScheduler;

    @Autowired
    private DiscordClient discordClient;

    public Reminder createReminder(MessageChannel channel, Member member, String message, Date date) {
        Reminder reminder = new Reminder();
        if (channel instanceof PrivateChannel) {
            reminder.setUserId(((PrivateChannel)channel).getUser().getId());
        } else {
            reminder.setGuildId(member.getGuild().getId());
            reminder.setUserId(member.getUser().getId());
        }
        reminder.setChannelId(channel.getId());
        reminder.setMessage(message);
        reminder.setDate(date);
        return schedule(reminderRepository.save(reminder));
    }

    private Reminder schedule(Reminder reminder) {
        taskScheduler.schedule(() -> {
            try {
                JDA jda = discordClient.getJda();
                MessageChannel channel = null;
                User user = jda.getUserById(reminder.getUserId());
                StringBuilder message = new StringBuilder();
                if (reminder.getGuildId() != null) {
                    Guild guild = jda.getGuildById(reminder.getGuildId());
                    if (guild != null) {
                        channel = guild.getTextChannelById(reminder.getChannelId());
                        if (user != null && guild.isMember(user)) {
                            message.append(user.getAsMention()).append(" ");
                        }
                    }
                } else {
                    channel = jda.getPrivateChannelById(reminder.getChannelId());
                }
                if (channel == null && user != null) {
                    channel = user.openPrivateChannel().complete();
                }
                if (channel != null) {
                    message.append(reminder.getMessage());
                    channel.sendMessage(message.toString()).queue();
                }
            } finally {
                reminderRepository.delete(reminder);
            }
        }, reminder.getDate());
        return reminder;
    }

    @Override
    public void onApplicationEvent(DiscordEvent readyEvent) {
        if (readyEvent.isType(ReadyEvent.class)) {
            reminderRepository.findAll().forEach(this::schedule);
        }
    }
}
