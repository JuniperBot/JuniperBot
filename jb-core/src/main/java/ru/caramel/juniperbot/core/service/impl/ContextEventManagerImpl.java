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
package ru.caramel.juniperbot.core.service.impl;

import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.EventListener;
import net.dv8tion.jda.core.hooks.IEventManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.caramel.juniperbot.core.listeners.DiscordEventListener;
import ru.caramel.juniperbot.core.service.CommandsService;
import ru.caramel.juniperbot.core.service.ContextService;
import ru.caramel.juniperbot.core.support.RequestScopedCacheManager;

import java.util.*;

@Service
public class ContextEventManagerImpl implements IEventManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContextEventManagerImpl.class);

    private final Set<EventListener> listeners = Collections.synchronizedSet(new HashSet<>());

    @Autowired
    private ContextService contextService;

    @Autowired
    private CommandsService commandsService;

    @Autowired
    @Qualifier(RequestScopedCacheManager.NAME)
    private RequestScopedCacheManager cacheManager;

    @Override
    public void register(Object listener) {
        if (!(listener instanceof EventListener)) {
            throw new IllegalArgumentException("Listener must implement EventListener");
        }
        listeners.add(((EventListener) listener));
    }

    @Override
    public void unregister(Object listener) {
        listeners.remove(listener);
    }

    @Override
    public void handle(Event event) {
        try {
            cacheManager.clear();
            contextService.initContext(event);
            if (event instanceof MessageReceivedEvent) {
                try {
                    commandsService.onMessageReceived((MessageReceivedEvent) event);
                } catch (Exception e) {
                    LOGGER.error("Could not process command", e);
                }
            }

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Handle event: " + event);
            }
            for (EventListener listener : listeners) {
                try {
                    listener.onEvent(event);
                } catch (Throwable throwable) {
                    LOGGER.error("One of the EventListeners had an uncaught exception", throwable);
                }
            }
            contextService.resetContext();
        } catch (Exception e) {
            LOGGER.error("Event manager caused an uncaught exception", e);
        } finally {
            cacheManager.clear();
        }
    }

    @Autowired
    public void registerContext(List<DiscordEventListener> listeners) {
        this.listeners.addAll(listeners);
    }

    @Override
    public List<Object> getRegisteredListeners() {
        return Collections.unmodifiableList(new LinkedList<>(listeners));
    }
}
