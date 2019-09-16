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
package ru.juniperbot.api.utils;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.RateLimiter;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("unstable")
public class IdentityRateLimiter<T> {

    private final double permitsPerSecond;

    private final LoadingCache<T, RateLimiter> limitersCache;

    public IdentityRateLimiter(double permitsPerSecond) {
        this.permitsPerSecond = permitsPerSecond;
        this.limitersCache = CacheBuilder.newBuilder()
                .concurrencyLevel(7)
                .expireAfterAccess(1, TimeUnit.HOURS)
                .build(
                        new CacheLoader<>() {
                            public RateLimiter load(@ParametersAreNonnullByDefault T identity) {
                                return RateLimiter.create(permitsPerSecond);
                            }
                        });
    }

    public boolean tryAcquire(T identity) {
        try {
            return limitersCache.get(identity).tryAcquire();
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
