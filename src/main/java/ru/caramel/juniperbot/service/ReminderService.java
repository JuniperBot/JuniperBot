package ru.caramel.juniperbot.service;

import java.util.Date;

import net.dv8tion.jda.core.entities.Channel;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
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

    public Reminder createReminder(Channel channel, Member member, String message, Date date) {
        Reminder reminder = new Reminder();
        reminder.setGuildId(member.getGuild().getId());
        reminder.setUserId(member.getUser().getId());
        reminder.setChannelId(channel.getId());
        reminder.setMessage(message);
        reminder.setDate(date);
        reminderRepository.save(reminder);
        return schedule(reminder);
    }

    private Reminder schedule(Reminder reminder) {
        /*taskScheduler.schedule(() -> {
            StringBuilder builder = new StringBuilder();

            Guild guild = discordClient.getJda().getGuildById(reminder.getGuildId());
            if (guild != null) {
                Channel
            }

            if (message.getGuild() != null && message.getGuild().isMember(message.getAuthor())) {
                builder.append(String.format("<@!%s> ", message.getAuthor().getId()));
            }
            builder.append(m.group(3));
            message.getChannel().sendMessage(builder.toString()).queue();
        }, date.toDate());*/

        return reminder;
    }

    @Override
    public void onApplicationEvent(DiscordEvent readyEvent) {
        if (readyEvent.isType(ReadyEvent.class)) {
            scheduleAll();
        }
    }

    public void scheduleAll() {

    }
}
