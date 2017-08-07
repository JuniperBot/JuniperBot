package ru.caramel.juniperbot.integration.discord;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.managers.AudioManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

@Service
public class DiscordBeanProviders {

    private ThreadLocal<MessageReceivedEvent> events = new ThreadLocal<>();

    public void setMessageContext(MessageReceivedEvent event) {
        events.set(event);
    }

    @Bean
    @Scope("prototype")
    public Guild getGuild() {
        MessageReceivedEvent event = events.get();
        return event != null ? event.getGuild() : null;
    }

    @Bean
    @Scope("prototype")
    public AudioManager getAudioManager() {
        Guild guild = getGuild();
        return guild != null ? guild.getAudioManager() : null;
    }
}
