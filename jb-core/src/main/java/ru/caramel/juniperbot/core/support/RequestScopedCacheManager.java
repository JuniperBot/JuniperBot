package ru.caramel.juniperbot.core.support;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RequestScopedCacheManager implements CacheManager {

    public static final String NAME = "requestScopeCacheManager";

    private static final ThreadLocal<Map<String, Cache>> threadLocalCache = ThreadLocal.withInitial(ConcurrentHashMap::new);

    @Override
    public Cache getCache(String name) {
        final Map<String, Cache> cacheMap = threadLocalCache.get();
        return cacheMap.computeIfAbsent(name, ConcurrentMapCache::new);
    }

    @Override
    public Collection<String> getCacheNames() {
        return threadLocalCache.get().keySet();
    }

    public void clear() {
        threadLocalCache.remove();
    }
}