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
package ru.caramel.juniperbot.core.common.persistence;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Type;
import ru.caramel.juniperbot.core.common.persistence.base.GuildEntity;

import javax.persistence.*;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "member")
public class LocalMember extends GuildEntity {
    private static final long serialVersionUID = -1439894653981742656L;

    @ManyToOne
    @JoinColumn(name="user_id")
    private LocalUser user;

    @Column(name = "effective_name")
    private String effectiveName;

    @Type(type = "jsonb")
    @Column(name = "last_known_roles", columnDefinition = "json")
    private List<Long> lastKnownRoles;

    @Transient
    public String getAsMention() {
        return StringUtils.isEmpty(effectiveName) ? user.getAsMention() : "<@!" + user.getUserId() + '>';
    }
}
