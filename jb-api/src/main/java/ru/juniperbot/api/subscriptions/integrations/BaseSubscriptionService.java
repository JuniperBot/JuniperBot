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
package ru.juniperbot.api.subscriptions.integrations;

import club.minnced.discord.webhook.send.WebhookMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.PropertyPlaceholderHelper;
import ru.juniperbot.common.model.request.WebhookRequest;
import ru.juniperbot.common.persistence.entity.WebHook;
import ru.juniperbot.common.persistence.entity.base.BaseSubscriptionEntity;
import ru.juniperbot.common.persistence.repository.WebHookRepository;
import ru.juniperbot.common.persistence.repository.base.BaseSubscriptionRepository;
import ru.juniperbot.common.service.ConfigService;
import ru.juniperbot.common.service.GatewayService;
import ru.juniperbot.common.utils.LocaleUtils;
import ru.juniperbot.common.utils.WebhookUtils;

import javax.annotation.PreDestroy;
import java.util.Locale;
import java.util.concurrent.ScheduledFuture;

@Slf4j
public abstract class BaseSubscriptionService<T extends BaseSubscriptionEntity, S, U> implements SubscriptionService<T, S, U> {

    protected static PropertyPlaceholderHelper PLACEHOLDER = new PropertyPlaceholderHelper("{", "}");

    @Autowired
    protected TaskScheduler scheduler;

    @Autowired
    protected WebHookRepository hookRepository;

    @Autowired
    protected GatewayService gatewayService;

    @Autowired
    protected ConfigService configService;

    @Autowired
    private ApplicationContext context;

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

    @Override
    public void init() {
        // default
    }

    @PreDestroy
    private void destroy() {
        if (updateTask != null) {
            updateTask.cancel(true);
        }
    }

    protected boolean notifyConnection(S subscription, T connection) {
        try {
            WebhookUtils.sendWebhook(connection.getWebHook(), createMessage(subscription, connection), e -> {
                e.setEnabled(false);
                hookRepository.save(e);
            });
        } catch (Exception e) {
            log.warn("Could not notify {}[id={}]", connection.getClass().getSimpleName(), connection.getId(), e);
            return false;
        }
        return true;
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
        gatewayService.deleteWebhook(new WebhookRequest(connection.getWebHook().getId(), connection.getGuildId()));
        repository.delete(connection);
    }

    protected String getMessage(T connection, String key, Object... args) {
        if (key == null) {
            return null;
        }
        Locale locale = LocaleUtils.getOrDefault(configService.getLocale(connection.getGuildId()));
        return context.getMessage(key, args, key, locale);
    }
}
