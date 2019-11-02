/*
 * This file is part of JuniperBot.
 *
 * JuniperBot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * JuniperBot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with JuniperBot. If not, see <http://www.gnu.org/licenses/>.
 */
package ru.juniperbot.common.worker.event.service;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.jmx.export.MBeanExportOperations;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import ru.juniperbot.common.support.jmx.ThreadPoolTaskExecutorMBean;
import ru.juniperbot.common.worker.configuration.WorkerProperties;
import ru.juniperbot.common.worker.event.DiscordEvent;
import ru.juniperbot.common.worker.event.intercept.EventFilterFactory;
import ru.juniperbot.common.worker.event.intercept.FilterChain;
import ru.juniperbot.common.worker.event.listeners.DiscordEventListener;

import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class ContextEventManagerImpl implements JbEventManager {

    private final List<EventListener> listeners = new ArrayList<>();

    private final Map<Class<?>, EventFilterFactory<?>> filterFactoryMap = new ConcurrentHashMap<>();

    private final Map<Integer, ThreadPoolTaskExecutor> shardExecutors = new ConcurrentHashMap<>();

    @Autowired
    private WorkerProperties workerProperties;

    @Autowired
    private ContextService contextService;

    @Autowired
    private MBeanExportOperations mBeanExportOperations;

    @Override
    public void handle(GenericEvent event) {
        if (workerProperties.getEvents().isAsyncExecution()) {
            try {
                getTaskExecutor(event.getJDA()).execute(() -> handleEvent(event));
            } catch (TaskRejectedException e) {
                log.debug("Event rejected: {}", event);
            }
        } else {
            handleEvent(event);
        }
    }

    private void handleEvent(GenericEvent event) {
        try {
            contextService.initContext(event);
            loopListeners(event);
        } catch (Exception e) {
            log.error("Event manager caused an uncaught exception", e);
        } finally {
            contextService.resetContext();
        }
    }

    private void loopListeners(GenericEvent event) {
        if (event instanceof GuildMessageReceivedEvent) {
            dispatchChain(GuildMessageReceivedEvent.class, (GuildMessageReceivedEvent) event);
        }
        for (EventListener listener : listeners) {
            try {
                listener.onEvent(event);
            } catch (ObjectOptimisticLockingFailureException e) {
                log.warn("[{}] optimistic lock happened for {}#{} while handling {}",
                        listener.getClass().getSimpleName(),
                        e.getPersistentClassName(),
                        e.getIdentifier(),
                        event);
            } catch (Throwable throwable) {
                log.error("[{}] had an uncaught exception for handling {}",
                        listener.getClass().getSimpleName(),
                        event,
                        throwable);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends Event> void dispatchChain(Class<T> type, T event) {
        EventFilterFactory<T> factory = (EventFilterFactory<T>) filterFactoryMap.get(type);
        if (factory == null) {
            return;
        }
        FilterChain<T> chain = factory.createChain(event);
        if (chain == null) {
            return;
        }

        try {
            chain.doFilter(event);
        } catch (Exception e) {
            log.error("Could not process filter chain", e);
        }
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

    @Autowired
    public void registerFilterFactories(List<EventFilterFactory> factories) {
        if (CollectionUtils.isNotEmpty(factories)) {
            factories.forEach(e -> filterFactoryMap.putIfAbsent(e.getType(), e));
        }
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

    private TaskExecutor getTaskExecutor(JDA shard) {
        return shardExecutors.computeIfAbsent(shard.getShardInfo().getShardId(), shardId -> {
            String name = String.format("%s Event-Executor", shard.getShardInfo());
            ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
            executor.setCorePoolSize(workerProperties.getEvents().getCorePoolSize());
            executor.setMaxPoolSize(workerProperties.getEvents().getMaxPoolSize());
            executor.setBeanName(name);
            executor.setThreadNamePrefix(name);
            executor.initialize();
            mBeanExportOperations.registerManagedResource(new ThreadPoolTaskExecutorMBean(name, executor));
            return executor;
        });
    }

    @PreDestroy
    public void destroy() {
        this.shardExecutors.values().forEach(ThreadPoolTaskExecutor::shutdown);
    }

    @Override
    public List<Object> getRegisteredListeners() {
        return Collections.unmodifiableList(listeners);
    }
}
