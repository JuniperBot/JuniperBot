package ru.caramel.juniperbot.service.impl;

import java.util.Date;

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.exceptions.PermissionException;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.caramel.juniperbot.integration.discord.DiscordClient;
import ru.caramel.juniperbot.integration.discord.model.DiscordEvent;
import ru.caramel.juniperbot.integration.discord.model.DiscordException;
import ru.caramel.juniperbot.persistence.entity.Reminder;
import ru.caramel.juniperbot.persistence.repository.ReminderRepository;
import ru.caramel.juniperbot.service.ReminderService;

@Service
public class ReminderServiceImpl implements ReminderService, ApplicationListener<DiscordEvent> {

    @Autowired
    private ReminderRepository repository;

    @Autowired
    private TaskScheduler taskScheduler;

    @Autowired
    private DiscordClient discordClient;

    @Transactional
    @Override
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
        return schedule(repository.save(reminder));
    }

    private Reminder schedule(Reminder reminder, Date date) {
        taskScheduler.schedule(() -> {
            try {
                JDA jda = discordClient.getJda();
                if (jda == null || !JDA.Status.CONNECTED.equals(jda.getStatus())) {
                    throw new DiscordException("Could not send reminder, not connected!");
                }

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
                repository.delete(reminder);
            } catch (PermissionException e) {
                repository.delete(reminder);
            } catch (Exception e) {
                // try to repeat schedule after 5 mins
                schedule(reminder, new DateTime(date).plusSeconds(300).toDate());
            }
        }, date);
        return reminder;
    }

    private Reminder schedule(Reminder reminder) {
        return schedule(reminder, reminder.getDate());
    }

    @Override
    public void onApplicationEvent(DiscordEvent readyEvent) {
        if (readyEvent.isType(ReadyEvent.class)) {
            repository.findAll().forEach(this::schedule);
        }
    }
}
