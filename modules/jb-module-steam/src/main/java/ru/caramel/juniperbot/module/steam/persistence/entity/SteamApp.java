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
package ru.caramel.juniperbot.module.steam.persistence.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ru.juniperbot.common.persistence.entity.base.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "steam_app")
@ToString
@Getter
@Setter
public class SteamApp extends BaseEntity {

    private static final long serialVersionUID = -7966161606616937933L;

    @Column(name = "app_id")
    private Long appId;

    @Column
    private String name;

    @Column(insertable = false)
    private String terms;
}
