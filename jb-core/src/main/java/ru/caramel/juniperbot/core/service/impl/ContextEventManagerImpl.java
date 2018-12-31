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

import com.codahale.metrics.Timer;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.EventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Service;
import ru.caramel.juniperbot.core.listeners.DiscordEventListener;
import ru.caramel.juniperbot.core.model.DiscordEvent;
import ru.caramel.juniperbot.core.service.CommandsService;
import ru.caramel.juniperbot.core.service.ContextService;
import ru.caramel.juniperbot.core.service.JbEventManager;
import ru.caramel.juniperbot.core.service.StatisticsService;
import ru.caramel.juniperbot.core.support.RequestScopedCacheManager;

import java.util.*;

@Slf4j
@Service
public class ContextEventManagerImpl implements JbEventManager {

    private final List<EventListener> listeners = new ArrayList<>();

    @Getter
    @Setter
    @Value("${discord.asyncEvents:true}")
    private boolean async;

    @Autowired
    private ContextService contextService;

    @Autowired
    private CommandsService commandsService;

    @Autowired
    private StatisticsService statisticsService;

    @Autowired
    @Qualifier(RequestScopedCacheManager.NAME)
    private RequestScopedCacheManager cacheManager;

    @Autowired
    @Qualifier("eventManagerExecutor")
    private TaskExecutor taskExecutor;

    @Override
    public void handle(Event event) {
        if (async) {
            try {
                taskExecutor.execute(() -> handleEvent(event));
            } catch (TaskRejectedException e) {
                log.debug("Event rejected: {}", event);
            }
        } else {
            handleEvent(event);
        }
    }

    private void handleEvent(Event event) {
        try {
            cacheManager.clear();
            contextService.initContext(event);
            if (statisticsService.isDetailed()) {
                statisticsService.doWithTimer(getTimer(event), () -> {
                    loopListeners(event);
                });
            } else {
                loopListeners(event);
            }
        } catch (Exception e) {
            log.error("Event manager caused an uncaught exception", e);
        } finally {
            contextService.resetContext();
            cacheManager.clear();
        }
    }

    private void loopListeners(Event event) {
        if (event instanceof MessageReceivedEvent) {
            try {
                commandsService.onMessageReceived((MessageReceivedEvent) event);
            } catch (Exception e) {
                log.error("Could not process command", e);
            }
        }

        for (EventListener listener : listeners) {
            try {
                listener.onEvent(event);
            } catch (Throwable throwable) {
                log.error("One of the EventListeners had an uncaught exception", throwable);
            }
        }
    }

    private Timer getTimer(Event event) {
        int shard = -1;
        if (event.getJDA() != null && event.getJDA().getShardInfo() != null) {
            shard = event.getJDA().getShardInfo().getShardId();
        }
        return statisticsService.getTimer(String.format("event/shard.%d/%s", shard, event.getClass().getName()));
    }

    private int compareListeners(EventListener first, EventListener second) {
        return getPriority(first) - getPriority(second);
    }

    private int getPriority(EventListener eventListener) {
        return eventListener != null && eventListener.getClass().isAnnotationPresent(DiscordEvent.class)
                ? eventListener.getClass().getAnnotation(DiscordEvent.class).priority()
                : Integer.MAX_VALUE;
    }

    @Override
    public void unregister(Object listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    @Override
    public void register(Object listener) {
        if (!(listener instanceof EventListener)) {
            throw new IllegalArgumentException("Listener must implement EventListener");
        }
        registerListeners(Collections.singletonList((EventListener) listener));
    }

    @Autowired
    public void registerContext(List<DiscordEventListener> listeners) {
        registerListeners(listeners);
    }

    private void registerListeners(List<? extends EventListener> listeners) {
        synchronized (this.listeners) {
            Set<EventListener> listenerSet = new HashSet<>(this.listeners);
            listenerSet.addAll(listeners);
            this.listeners.clear();
            this.listeners.addAll(listenerSet);
            this.listeners.sort(this::compareListeners);
        }
    }

    @Override
    public List<Object> getRegisteredListeners() {
        return Collections.unmodifiableList(listeners);
    }
}
