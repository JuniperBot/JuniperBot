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
import lombok.ToString;
import org.hibernate.annotations.Type;
import ru.juniperbot.common.model.steam.SteamAppDetails;
import ru.juniperbot.common.persistence.entity.base.BaseEntity;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "steam_cache")
@ToString
@Getter
@Setter
public class SteamCache extends BaseEntity {

    private static final long serialVersionUID = -7528801874156064861L;

    @Column(name = "app_id")
    private Long appId;

    private String locale;

    @Type(type = "jsonb")
    @Column(columnDefinition = "json")
    private SteamAppDetails details;

    @Column(name = "update_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updateDate;

}
