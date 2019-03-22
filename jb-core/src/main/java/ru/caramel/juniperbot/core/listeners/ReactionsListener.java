/*
 * This file is part of JuniperBotJ.
 *
 * JuniperBotJ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * JuniperBotJ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with JuniperBotJ. If not, see <http://www.gnu.org/licenses/>.
 */
package ru.caramel.juniperbot.core.listeners;

import net.dv8tion.jda.core.events.message.MessageDeleteEvent;
import net.dv8tion.jda.core.events.message.react.GenericMessageReactionEvent;
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import ru.caramel.juniperbot.core.event.DiscordEvent;
import ru.caramel.juniperbot.core.event.service.ContextService;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

@DiscordEvent
public class ReactionsListener extends DiscordEventListener {

    public static final String[] CHOICES = new String[]{"1⃣", "2⃣", "3⃣", "4⃣", "5⃣", "6⃣", "7⃣", "8⃣", "9⃣", "\uD83D\uDD1F"};

    private Map<String, Function<MessageReactionAddEvent, Boolean>> addListeners = new ConcurrentHashMap<>();

    private Map<String, BiFunction<GenericMessageReactionEvent, Boolean, Boolean>> listeners = new ConcurrentHashMap<>();

    private Map<String, Long> messageTtl = new ConcurrentHashMap<>();

    @Autowired
    private ContextService contextService;

    @Value("${core.listenerTtlMs:3600000}")
    private long listenerTtlMs;

    @Override
    public void onGenericMessageReaction(GenericMessageReactionEvent event) {
        BiFunction<GenericMessageReactionEvent, Boolean, Boolean> handler = listeners.get(event.getMessageId());
        boolean add = event instanceof MessageReactionAddEvent;
        if (handler != null) {
            contextService.withContextAsync(event.getGuild(), () -> {
                if (Boolean.TRUE.equals(handler.apply(event, add))) {
                    listeners.remove(event.getMessageId());
                }
            });
        }

        if (add) {
            Function<MessageReactionAddEvent, Boolean> addHandler = addListeners.get(event.getMessageId());
            if (addHandler != null) {
                contextService.withContextAsync(event.getGuild(), () -> {
                    if (Boolean.TRUE.equals(addHandler.apply((MessageReactionAddEvent) event))) {
                        addListeners.remove(event.getMessageId());
                    }
                });
            }
        }
    }

    @Scheduled(fixedDelay = 3600000)
    public void monitor() {
        long currentTimeMillis = System.currentTimeMillis();
        Set<String> oldListeners = messageTtl.entrySet().stream()
                .filter(e -> currentTimeMillis - e.getValue() > listenerTtlMs)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        unsubscribeAll(oldListeners);
    }

    @Override
    public void onMessageDelete(MessageDeleteEvent event) {
        unsubscribe(event.getMessageId());
    }

    public void onReactionAdd(String message, Function<MessageReactionAddEvent, Boolean> handler) {
        addListeners.put(message, handler);
        messageTtl.put(message, System.currentTimeMillis());
    }

    public void onReaction(String message, BiFunction<GenericMessageReactionEvent, Boolean, Boolean> handler) {
        listeners.put(message, handler);
        messageTtl.put(message, System.currentTimeMillis());
    }

    public void unsubscribe(String messageId) {
        addListeners.remove(messageId);
        listeners.remove(messageId);
        messageTtl.remove(messageId);
    }

    public void unsubscribeAll(Collection<String> messageIds) {
        messageIds.forEach(this::unsubscribe);
    }
}
