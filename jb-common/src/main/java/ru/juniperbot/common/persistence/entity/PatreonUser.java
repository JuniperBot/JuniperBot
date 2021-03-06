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
package ru.juniperbot.common.persistence.entity;

import lombok.Getter;
import lombok.Setter;
import ru.juniperbot.common.persistence.entity.base.FeaturedUserEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Getter
@Setter
@Entity
@Table(name = "patreon_user", schema = "public")
public class PatreonUser extends FeaturedUserEntity {
    private static final long serialVersionUID = -1431894653981742651L;

    @Column
    private String patreonId;

    @Column
    private boolean active;

    @Column
    private Long boostedGuildId;
}
