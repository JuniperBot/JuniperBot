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
package ru.caramel.juniperbot.core.persistence.repository.base;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;
import ru.caramel.juniperbot.core.persistence.entity.base.TextChannelEntity;

import java.util.List;

@NoRepositoryBean
public interface TextChannelRepository<T extends TextChannelEntity> extends GuildRepository<T> {

    List<T> findByGuildIdAndChannelId(String guildId, String channelId);

    @Query("SELECT count(e) > 0 FROM #{#entityName} e WHERE e.channelId = :channelId AND e.guildId = :guildId")
    boolean exists(@Param("guildId") String guildId, @Param("channelId") String channelId);

    Long deleteByGuildIdAndChannelId(String guildId, String channelId);

    Long deleteByGuildId(String guildId);

}
