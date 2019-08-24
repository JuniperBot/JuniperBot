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
package ru.juniperbot.common.persistence.repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.juniperbot.common.persistence.entity.LocalMember;
import ru.juniperbot.common.persistence.entity.MemberWarning;
import ru.juniperbot.common.persistence.repository.base.GuildRepository;

import java.util.List;

@Repository
public interface MemberWarningRepository extends GuildRepository<MemberWarning> {

    @Query("SELECT e FROM MemberWarning e WHERE e.active = true AND e.guildId = :guildId AND e.violator = :violator ORDER BY e.date")
    List<MemberWarning> findActiveByViolator(@Param("guildId") long guildId, @Param("violator") LocalMember violator);

    @Query("SELECT count(e) FROM MemberWarning e WHERE e.active = true AND e.guildId = :guildId AND e.violator = :violator")
    long countActiveByViolator(@Param("guildId") long guildId, @Param("violator") LocalMember violator);

    @Modifying
    @Query("UPDATE MemberWarning e SET e.active = false WHERE e.active = true AND e.guildId = :guildId AND e.violator = :violator")
    int flushWarnings(@Param("guildId") long guildId, @Param("violator") LocalMember violator);

}
