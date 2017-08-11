package ru.caramel.juniperbot.integration.discord.model;

import net.dv8tion.jda.core.events.Event;
import org.springframework.context.ApplicationEvent;

public class DiscordEvent extends ApplicationEvent {

    public DiscordEvent(Event event) {
        super(event);
    }

    @Override
    public Event getSource()  {
        return (Event) super.getSource();
    }

    public boolean isType(Class<?> type) {
        return type.isAssignableFrom(getSource().getClass());
    }
}
