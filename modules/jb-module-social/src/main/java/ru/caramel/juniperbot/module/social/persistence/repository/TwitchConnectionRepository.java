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
package ru.caramel.juniperbot.module.social.persistence.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.juniperbot.common.persistence.repository.base.BaseSubscriptionRepository;
import ru.caramel.juniperbot.module.social.persistence.entity.TwitchConnection;

import java.util.List;
import java.util.Set;

@Repository
public interface TwitchConnectionRepository extends BaseSubscriptionRepository<TwitchConnection> {

    @Query("SELECT DISTINCT t.userId FROM TwitchConnection t WHERE t.webHook IN (SELECT w FROM WebHook w WHERE w.enabled = true AND w.hookId IS NOT NULL AND w.token IS NOT NULL)")
    List<Long> findActiveUserIds();

    @Query("SELECT t FROM TwitchConnection t WHERE t.userId IN :userIds AND t.webHook IN (SELECT w FROM WebHook w WHERE w.enabled = true AND w.hookId IS NOT NULL AND w.token IS NOT NULL)")
    List<TwitchConnection> findActiveConnections(@Param("userIds") Set<Long> userIds);
}
