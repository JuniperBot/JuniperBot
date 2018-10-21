package ru.caramel.juniperbot.core.service.impl;

import net.dv8tion.jda.webhook.WebhookMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.PropertyPlaceholderHelper;
import ru.caramel.juniperbot.core.persistence.entity.WebHook;
import ru.caramel.juniperbot.core.persistence.entity.base.BaseSubscriptionEntity;
import ru.caramel.juniperbot.core.persistence.repository.WebHookRepository;
import ru.caramel.juniperbot.core.persistence.repository.base.BaseSubscriptionRepository;
import ru.caramel.juniperbot.core.service.*;

import javax.annotation.PreDestroy;
import java.util.concurrent.ScheduledFuture;

public abstract class BaseSubscriptionService<T extends BaseSubscriptionEntity, S, U> implements SubscriptionService<T, S, U> {

    private static final Logger log = LoggerFactory.getLogger(BaseSubscriptionService.class);

    protected static PropertyPlaceholderHelper PLACEHOLDER = new PropertyPlaceholderHelper("{", "}");

    @Autowired
    protected WebHookService webHookService;

    @Autowired
    protected MessageService messageService;

    @Autowired
    protected ContextService contextService;

    @Autowired
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
    public void delete(T connection) {
        webHookService.delete(connection.getGuildId(), connection.getWebHook());
        repository.delete(connection);
    }
}
