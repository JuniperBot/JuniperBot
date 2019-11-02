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

import javax.persistence.*;
import java.util.Date;

@Getter
@Setter
@Entity
@Table(name = "user", schema = "public")
public class LocalUser extends FeaturedUserEntity {
    private static final long serialVersionUID = -1439894653981742651L;

    @Column
    private String name;

    @Column
    private String discriminator;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "last_online_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastOnline;

    @Transient
    public String getAsMention() {
        return "<@" + userId + '>';
    }
}
