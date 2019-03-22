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
package ru.caramel.juniperbot.core.moderation.persistence;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.caramel.juniperbot.core.common.persistence.LocalMember;
import ru.caramel.juniperbot.core.common.persistence.base.GuildEntity;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.util.Date;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "member_warning")
public class MemberWarning extends GuildEntity {

    private static final long serialVersionUID = 9200793551394006363L;

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH, CascadeType.DETACH})
    @JoinColumn(name = "moderator_id")
    private LocalMember moderator;

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH, CascadeType.DETACH})
    @JoinColumn(name = "violator_id")
    private LocalMember violator;

    @Column
    @Size(max = 1000)
    private String reason;

    @Column
    private boolean active;

    @Column
    @Temporal(TemporalType.TIMESTAMP)
    private Date date;

    public MemberWarning(long guildId, LocalMember moderator, LocalMember violator, String reason) {
        this.guildId = guildId;
        this.moderator = moderator;
        this.violator = violator;
        this.reason = reason;
        this.active = true;
        this.date = new Date();
    }
}
