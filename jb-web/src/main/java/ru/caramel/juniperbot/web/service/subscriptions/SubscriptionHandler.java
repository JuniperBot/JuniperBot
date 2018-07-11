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
package ru.caramel.juniperbot.web.service.subscriptions;

import ru.caramel.juniperbot.web.dto.api.config.SubscriptionDto;
import ru.caramel.juniperbot.web.model.SubscriptionType;

import java.util.Map;

public interface SubscriptionHandler<T> {

    SubscriptionDto getSubscription(T object);

    SubscriptionDto create(long fuildId, Map<String, ?> data);

    boolean update(SubscriptionDto object);

    void delete(long id);

    Class<T> getEntityType();

    SubscriptionType getType();
}
