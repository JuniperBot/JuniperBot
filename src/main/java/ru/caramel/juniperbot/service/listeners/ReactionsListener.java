package ru.caramel.juniperbot.service.listeners;

import net.dv8tion.jda.core.events.message.MessageDeleteEvent;
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent;
import org.springframework.stereotype.Component;
import ru.caramel.juniperbot.integration.discord.DiscordEventListener;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Component
public class ReactionsListener extends DiscordEventListener {

    private Map<String, Function<MessageReactionAddEvent, Boolean>> listeners = new ConcurrentHashMap<>();

    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        Function<MessageReactionAddEvent, Boolean> handler = listeners.get(event.getMessageId());
        if (handler != null && Boolean.TRUE.equals(handler.apply(event))) {
            listeners.remove(event.getMessageId());
        }
    }

    @Override
    public void onMessageDelete(MessageDeleteEvent event) {
        listeners.remove(event.getMessageId());
    }

    public void onReaction(String message, Function<MessageReactionAddEvent, Boolean> handler) {
        listeners.put(message, handler);
    }
}
