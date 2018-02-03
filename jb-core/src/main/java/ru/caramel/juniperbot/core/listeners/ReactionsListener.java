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

import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.events.message.MessageDeleteEvent;
import net.dv8tion.jda.core.events.message.react.GenericMessageReactionEvent;
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.caramel.juniperbot.core.service.ContextService;
import ru.caramel.juniperbot.core.service.DiscordService;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;

@Component
public class ReactionsListener extends DiscordEventListener {

    public static final String[] CHOICES = new String[]{"1⃣", "2⃣", "3⃣", "4⃣", "5⃣", "6⃣", "7⃣", "8⃣", "9⃣", "\uD83D\uDD1F"};

    private Map<String, Function<MessageReactionAddEvent, Boolean>> addListeners = new ConcurrentHashMap<>();

    private Map<String, BiFunction<GenericMessageReactionEvent, Boolean, Boolean>> listeners = new ConcurrentHashMap<>();

    @Autowired
    private ContextService contextService;

    @Autowired
    private DiscordService discordService;

    @Override
    public void onGenericMessageReaction(GenericMessageReactionEvent event) {
        boolean contextInitialized = false;
        if (event.isFromType(ChannelType.PRIVATE) && !Objects.equals(discordService.getJda(), event.getJDA())) {
            // If we have multiple shards, the MessageReactionAddEvent event
            // of PrivateChannel is fired twice for each JDA instance (shard).
            return;
        }

        BiFunction<GenericMessageReactionEvent, Boolean, Boolean> handler = listeners.get(event.getMessageId());
        boolean add = event instanceof MessageReactionAddEvent;
        if (handler != null) {
            if (event.getGuild() != null) {
                contextService.initContext(event.getGuild());
                contextInitialized = true;
            }
            if (Boolean.TRUE.equals(handler.apply(event, add))) {
                listeners.remove(event.getMessageId());
            }
        }

        if (add) {
            Function<MessageReactionAddEvent, Boolean> addHandler = addListeners.get(event.getMessageId());
            if (addHandler != null) {
                if (!contextInitialized && event.getGuild() != null) {
                    contextService.initContext(event.getGuild());
                    contextInitialized = true;
                }
                if (Boolean.TRUE.equals(addHandler.apply((MessageReactionAddEvent) event))) {
                    addListeners.remove(event.getMessageId());
                }
            }
        }

        if (contextInitialized) {
            contextService.resetContext();
        }
    }

    @Override
    public void onMessageDelete(MessageDeleteEvent event) {
        addListeners.remove(event.getMessageId());
        listeners.remove(event.getMessageId());
    }

    public void onReactionAdd(String message, Function<MessageReactionAddEvent, Boolean> handler) {
        addListeners.put(message, handler);
    }

    public void onReaction(String message, BiFunction<GenericMessageReactionEvent, Boolean, Boolean> handler) {
        listeners.put(message, handler);
    }

    public void unsubscribeAll(Collection<String> messageIds) {
        messageIds.forEach(id -> {
            addListeners.remove(id);
            listeners.remove(id);
        });
    }
}
