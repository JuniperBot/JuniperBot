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
package ru.caramel.juniperbot.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.caramel.juniperbot.persistence.entity.RankingConfig;

@Repository
public interface RankingConfigRepository extends JpaRepository<RankingConfig, Long> {

    @Query("SELECT r FROM RankingConfig r WHERE r.guildConfig.guildId = :guildId")
    RankingConfig findByGuildId(@Param("guildId") long guildId);

    @Query("select count(r) > 0 FROM RankingConfig r WHERE r.guildConfig.guildId = :guildId AND r.enabled = true")
    boolean isEnabled(@Param("guildId") long guildId);
}
