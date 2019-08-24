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

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.juniperbot.common.persistence.entity.GuildConfig;
import ru.juniperbot.common.persistence.repository.base.GuildRepository;

@Repository
public interface GuildConfigRepository extends GuildRepository<GuildConfig> {

    @Query("SELECT e.prefix FROM GuildConfig e WHERE e.guildId = :guildId")
    String findPrefixByGuildId(@Param("guildId") long guildId);

    @Query("SELECT e.locale FROM GuildConfig e WHERE e.guildId = :guildId")
    String findLocaleByGuildId(@Param("guildId") long guildId);

    @Query("SELECT e.color FROM GuildConfig e WHERE e.guildId = :guildId")
    String findColorByGuildId(@Param("guildId") long guildId);
}
