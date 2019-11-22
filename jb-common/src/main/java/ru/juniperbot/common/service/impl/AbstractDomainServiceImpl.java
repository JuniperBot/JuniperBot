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
package ru.juniperbot.common.service.impl;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import ru.juniperbot.common.persistence.entity.base.GuildEntity;
import ru.juniperbot.common.persistence.repository.base.GuildRepository;
import ru.juniperbot.common.service.DomainService;
import ru.juniperbot.common.service.GatewayService;
import ru.juniperbot.common.support.JbCacheManager;

@Slf4j
public abstract class AbstractDomainServiceImpl<T extends GuildEntity, R extends GuildRepository<T>> implements DomainService<T> {

    private final Object $lock = new Object[0];

    protected final R repository;

    @Getter
    @Setter
    protected boolean cacheable;

    @Autowired
    protected JbCacheManager cacheManager;

    @Autowired
    protected ApplicationContext applicationContext;

    @Autowired
    protected TransactionTemplate transactionTemplate;

    @Autowired
    protected GatewayService gatewayService;

    protected AbstractDomainServiceImpl(R repository) {
        this(repository, false);
    }

    protected AbstractDomainServiceImpl(R repository, boolean cacheable) {
        this.repository = repository;
        this.cacheable = cacheable;
    }

    @Override
    @Transactional(readOnly = true)
    public T get(Guild guild) {
        return getByGuildId(guild.getIdLong());
    }

    @Override
    @Transactional(readOnly = true)
    public T get(long id) {
        return repository.findById(id).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public T getByGuildId(long guildId) {
        if (cacheable) {
            return cacheManager.get(getDomainClass(), guildId, repository::findByGuildId);
        }
        return repository.findByGuildId(guildId);
    }

    @Override
    @Transactional
    public T save(T entity) {
        T result = repository.save(entity);
        evict(entity.getGuildId());
        return result;
    }

    @Transactional(readOnly = true)
    @Override
    public boolean exists(long guildId) {
        return repository.existsByGuildId(guildId);
    }

    @Override
    public void evict(long guildId) {
        gatewayService.evictCache(getDomainClass().getName(), guildId);
        if (cacheable) {
            cacheManager.evict(getDomainClass(), guildId);
        }
    }

    @Override
    @Transactional
    public T getOrCreate(long guildId) {
        T result = repository.findByGuildId(guildId);
        if (result == null) {
            synchronized ($lock) {
                result = repository.findByGuildId(guildId);
                if (result == null) {
                    result = createNew(guildId);
                    repository.saveAndFlush(result);
                }
            }
        }
        return result;
    }

    @Override
    public void inTransaction(Runnable action) {
        try {
            transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus status) {
                    action.run();
                }
            });
        } catch (ObjectOptimisticLockingFailureException e) {
            log.warn("Optimistic locking failed for object {} [id={}]", e.getPersistentClassName(), e.getIdentifier());
        }
    }

    protected abstract T createNew(long guildId);

    protected abstract Class<T> getDomainClass();
}
