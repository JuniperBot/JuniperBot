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

import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.core.entities.Guild;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import ru.caramel.juniperbot.core.persistence.entity.base.GuildEntity;
import ru.caramel.juniperbot.core.persistence.repository.base.GuildRepository;
import ru.caramel.juniperbot.core.service.DomainService;
import ru.caramel.juniperbot.core.support.JbCacheManager;

public abstract class AbstractDomainServiceImpl<T extends GuildEntity, R extends GuildRepository<T>> implements DomainService<T> {

    protected final R repository;

    @Getter
    @Setter
    protected boolean cacheable;

    @Autowired
    protected JbCacheManager cacheManager;

    protected AbstractDomainServiceImpl(R repository) {
        this(repository, false);
    }

    protected AbstractDomainServiceImpl(R repository, boolean cacheable) {
        this.repository = repository;
        this.cacheable = cacheable;
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
    @Transactional(readOnly = true)
    public T get(long id) {
        return repository.findOne(id);
    }

    @Override
    @Transactional
    public T get(Guild guild) {
        Assert.notNull(guild, "Guild cannot be null");
        return getByGuildId(guild.getIdLong());
    }

    @Override
    @Transactional
    public T save(T entity) {
        T result = repository.save(entity);
        if (cacheable) {
            cacheManager.evict(getDomainClass(), entity.getGuildId());
        }
        return result;
    }

    @Transactional(readOnly = true)
    @Override
    public boolean exists(long guildId) {
        return repository.existsByGuildId(guildId);
    }

    @Override
    @Transactional
    public T getOrCreate(Guild guild) {
        return getOrCreate(guild.getIdLong());
    }

    @Override
    @Transactional
    public T getOrCreate(long guildId) {
        T result = getByGuildId(guildId);
        if (result == null) {
            synchronized (this) {
                result = getByGuildId(guildId);
                if (result == null) {
                    result = createNew(guildId);
                    repository.saveAndFlush(result);
                }
            }
        }
        return result;
    }

    protected abstract T createNew(long guildId);

    protected abstract Class<T> getDomainClass();
}
