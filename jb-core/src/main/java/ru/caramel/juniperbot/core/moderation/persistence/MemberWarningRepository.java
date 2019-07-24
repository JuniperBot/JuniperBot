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
package ru.caramel.juniperbot.core.moderation.persistence;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.caramel.juniperbot.core.common.persistence.LocalMember;
import ru.caramel.juniperbot.core.common.persistence.base.GuildRepository;

import java.util.List;

@Repository
public interface MemberWarningRepository extends GuildRepository<MemberWarning> {

    @Query("SELECT e FROM MemberWarning e WHERE e.guildId = :guildId AND e.violator = :violator ORDER BY e.date")
    List<MemberWarning> findByViolator(@Param("guildId") long guildId, @Param("violator") LocalMember violator);

    @Query("SELECT count(e) FROM MemberWarning e WHERE e.guildId = :guildId AND e.violator = :violator")
    long countByViolator(@Param("guildId") long guildId, @Param("violator") LocalMember violator);

}
