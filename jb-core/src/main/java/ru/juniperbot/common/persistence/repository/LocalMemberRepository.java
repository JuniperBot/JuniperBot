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
package ru.juniperbot.common.persistence.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.juniperbot.common.persistence.repository.base.GuildRepository;
import ru.juniperbot.common.persistence.entity.LocalMember;

import java.util.List;

@Repository
public interface LocalMemberRepository extends GuildRepository<LocalMember> {

    @Query("SELECT m FROM LocalMember m WHERE m.guildId = :guildId AND m.user.userId = :userId")
    LocalMember findByGuildIdAndUserId(@Param("guildId") long guildId, @Param("userId") String userId);

    @Query("SELECT m FROM LocalMember m WHERE m.guildId = :guildId AND (m.user.userId = :query OR UPPER(m.effectiveName) LIKE CONCAT('%',UPPER(:query),'%') OR UPPER(m.user.name) LIKE CONCAT('%',UPPER(:query),'%'))")
    List<LocalMember> findLike(@Param("guildId") long guildId, @Param("query") String query);

}
