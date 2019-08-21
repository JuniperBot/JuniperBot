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
package ru.juniperbot.common.service;

import net.dv8tion.jda.api.entities.Guild;
import ru.juniperbot.common.persistence.entity.base.GuildEntity;

public interface DomainService<T extends GuildEntity> {

    T get(Guild guild);

    T get(long id);

    T getByGuildId(long guildId);

    T getOrCreate(long guildId);

    T save(T entity);

    boolean exists(long guildId);

    boolean isCacheable();

    void setCacheable(boolean cacheable);

    void inTransaction(Runnable action);
}
