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
package ru.juniperbot.worker.rabbit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.juniperbot.common.configuration.RabbitConfiguration;
import ru.juniperbot.common.model.request.CacheEvictRequest;
import ru.juniperbot.common.support.JbCacheManager;

@EnableRabbit
@Component
@Slf4j
public class CacheEvictQueueListener extends BaseQueueListener {

    @Autowired
    private JbCacheManager cacheManager;

    @RabbitListener(queues = RabbitConfiguration.QUEUE_CACHE_EVICT_REQUEST)
    public void evictCache(CacheEvictRequest request) {
        cacheManager.evict(request.getCacheName(), request.getGuildId());
    }
}
