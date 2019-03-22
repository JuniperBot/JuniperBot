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
package ru.caramel.juniperbot.core.subscription.service;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.webhook.WebhookMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.PropertyPlaceholderHelper;
import ru.caramel.juniperbot.core.common.service.DiscordService;
import ru.caramel.juniperbot.core.configuration.SchedulerConfiguration;
import ru.caramel.juniperbot.core.event.service.ContextService;
import ru.caramel.juniperbot.core.message.service.MessageService;
import ru.caramel.juniperbot.core.subscription.persistence.BaseSubscriptionEntity;
import ru.caramel.juniperbot.core.subscription.persistence.BaseSubscriptionRepository;
import ru.caramel.juniperbot.core.subscription.persistence.WebHook;
import ru.caramel.juniperbot.core.subscription.persistence.WebHookRepository;

import javax.annotation.PreDestroy;
import java.util.concurrent.ScheduledFuture;

@Slf4j
public abstract class BaseSubscriptionService<T extends BaseSubscriptionEntity, S, U> implements SubscriptionService<T, S, U> {

    protected static PropertyPlaceholderHelper PLACEHOLDER = new PropertyPlaceholderHelper("{", "}");

    @Autowired
    protected WebHookService webHookService;

    @Autowired
    protected MessageService messageService;

    @Autowired
    protected ContextService contextService;

    @Autowired
    @Qualifier(SchedulerConfiguration.COMMON_SCHEDULER_NAME)
    protected TaskScheduler scheduler;

    @Autowired
    protected WebHookRepository hookRepository;

    @Autowired
    protected DiscordService discordService;

    protected final BaseSubscriptionRepository<T> repository;

    private ScheduledFuture<?> updateTask;

    public BaseSubscriptionService(BaseSubscriptionRepository<T> repository) {
        this.repository = repository;
    }

    protected synchronized void schedule(long updateInterval) {
        if (updateTask != null && !updateTask.isCancelled()) {
            throw new IllegalStateException("Update already scheduled");
        }
        updateTask = scheduler.scheduleWithFixedDelay(this::update, updateInterval);
    }

    @PreDestroy
    private void destroy() {
        if (updateTask != null) {
            updateTask.cancel(true);
        }
    }

    protected void notifyConnection(S subscription, T connection) {
        try {
            contextService.withContext(connection.getGuildId(), () -> {
                discordService.executeWebHook(connection.getWebHook(), createMessage(subscription, connection), e -> {
                    e.setEnabled(false);
                    hookRepository.save(e);
                });
            });
        } catch (Exception ex) {
            log.warn("Could not notify {}[id={}]", connection.getClass().getSimpleName(), connection.getId(), ex);
        }
    }

    protected abstract WebhookMessage createMessage(S subscription, T connection);

    protected abstract T createConnection(U user);

    protected void update() {
        // overrideable
    }

    @Override
    @Transactional
    public T create(long guildId, U user) {
        T connection = createConnection(user);
        connection.setGuildId(guildId);
        WebHook hook = new WebHook();
        hook.setEnabled(true);
        connection.setWebHook(hook);
        return repository.save(connection);
    }

    @Override
    @Transactional(readOnly = true)
    public T find(long id) {
        return repository.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public T save(T connection) {
        return repository.save(connection);
    }

    @Override
    @Transactional
    public void delete(T connection) {
        webHookService.delete(connection.getGuildId(), connection.getWebHook());
        repository.delete(connection);
    }
}
