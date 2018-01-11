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
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent;
import org.springframework.stereotype.Component;
import ru.caramel.juniperbot.core.service.listeners.DiscordEventListener;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Component
public class ReactionsListener extends DiscordEventListener {

    public static final String[] CHOICES = new String[]{"1⃣", "2⃣", "3⃣", "4⃣", "5⃣", "6⃣", "7⃣", "8⃣", "9⃣", "\uD83D\uDD1F"};

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
