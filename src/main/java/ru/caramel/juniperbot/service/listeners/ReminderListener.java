package ru.caramel.juniperbot.service.listeners;

import net.dv8tion.jda.core.events.ReadyEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.caramel.juniperbot.integration.discord.DiscordEventListener;
import ru.caramel.juniperbot.service.ReminderService;

@Component
public class ReminderListener extends DiscordEventListener {

    @Autowired
    private ReminderService reminderService;

    @Override
    public void onReady(ReadyEvent event) {
        reminderService.loadAll();
    }
}
