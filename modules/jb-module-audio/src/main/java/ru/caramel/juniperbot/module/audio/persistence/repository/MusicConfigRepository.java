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
package ru.caramel.juniperbot.module.audio.persistence.repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.caramel.juniperbot.core.persistence.repository.GuildOwnedRepository;
import ru.caramel.juniperbot.module.audio.persistence.entity.MusicConfig;

@Repository
public interface MusicConfigRepository extends GuildOwnedRepository<MusicConfig> {

    @Modifying
    @Transactional // it's bad to manage transactions on repository layer but in this usage case it doesn't break anything
    @Query("UPDATE MusicConfig m SET m.voiceVolume = :voiceVolume WHERE m.guildConfig.id = (SELECT g.id FROM GuildConfig g WHERE g.guildId = :guildId)")
    void updateVolume(@Param("guildId") long guildId, @Param("voiceVolume") int voiceVolume);

}
