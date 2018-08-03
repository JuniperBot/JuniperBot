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
package ru.caramel.juniperbot.core.support;

import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import ru.caramel.juniperbot.core.persistence.entity.base.BaseEntity;

import java.util.function.Function;

public class JbCacheManagerImpl extends ConcurrentMapCacheManager implements JbCacheManager {

    @Override
    public <T extends BaseEntity> T get(Class<T> clazz, Long id, Function<Long, T> supplier) {
        return get(getCacheName(clazz), id, supplier);
    }

    @Override
    public <T extends BaseEntity> void evict(Class<T> clazz, Long id) {
        evict(getCacheName(clazz), id);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T, K> T get(String cacheName, K key, Function<K, T> supplier) {
        Cache cache = getCache(cacheName);
        Cache.ValueWrapper valueWrapper = cache.get(key);
        if (valueWrapper != null && valueWrapper.get() != null) {
            return (T) valueWrapper.get();
        }
        T value = supplier.apply(key);
        cache.put(key, value);
        return value;
    }

    @Override
    public <K> void evict(String cacheName, K key) {
        getCache(cacheName).evict(key);
    }

    private String getCacheName(Class<?> clazz) {
        return clazz.getName();
    }
}
