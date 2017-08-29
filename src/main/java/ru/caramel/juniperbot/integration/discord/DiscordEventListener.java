package ru.caramel.juniperbot.integration.discord;

import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.springframework.context.ApplicationListener;
import ru.caramel.juniperbot.integration.discord.model.DiscordEvent;

public abstract class DiscordEventListener extends ListenerAdapter implements ApplicationListener<DiscordEvent> {

    @Override
    public void onApplicationEvent(DiscordEvent event) {
        onEvent(event.getSource());
    }
}
