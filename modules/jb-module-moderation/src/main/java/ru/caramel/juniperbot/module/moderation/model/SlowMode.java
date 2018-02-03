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
package ru.caramel.juniperbot.module.moderation.model;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.TimeUnit;

@Getter
@Setter
public class SlowMode {

    private long channelId;

    private long interval;

    private Cache<String, Object> coolDowns;

    public void setInterval(long interval) {
        if (coolDowns != null) {
            coolDowns.invalidateAll();
            coolDowns.cleanUp();
        }
        coolDowns = CacheBuilder.newBuilder()
                .concurrencyLevel(4)
                .expireAfterWrite(interval, TimeUnit.SECONDS)
                .build();
    }

    public boolean tick(String userId) {
        boolean blocked = coolDowns.getIfPresent(userId) != null;
        if (!blocked) {
            coolDowns.put(userId, new Object());
        }
        return blocked;
    }
}
