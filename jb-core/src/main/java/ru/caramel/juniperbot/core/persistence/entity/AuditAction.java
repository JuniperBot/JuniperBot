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
package ru.caramel.juniperbot.core.persistence.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections4.MapUtils;
import org.hibernate.annotations.Type;
import ru.caramel.juniperbot.core.model.enums.AuditActionType;
import ru.caramel.juniperbot.core.persistence.entity.base.GuildEntity;
import ru.caramel.juniperbot.core.persistence.entity.base.NamedReference;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.Map;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "audit_action")
public class AuditAction extends GuildEntity {

    private static final long serialVersionUID = 1252650749958531237L;

    @Column(name = "action_date")
    @Temporal(TemporalType.TIMESTAMP)
    @NotNull
    private Date actionDate;

    @Column(name = "action_type")
    @Enumerated(EnumType.STRING)
    @NotNull
    private AuditActionType actionType;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "id", column = @Column(name = "source_user_id", length = 21)),
            @AttributeOverride(name = "name", column = @Column(name = "source_user_name"))
    })
    private NamedReference user;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "id", column = @Column(name = "target_user_id", length = 21)),
            @AttributeOverride(name = "name", column = @Column(name = "target_user_name"))
    })
    private NamedReference targetUser;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "id", column = @Column(name = "channel_id", length = 21)),
            @AttributeOverride(name = "name", column = @Column(name = "channel_name"))
    })
    private NamedReference channel;

    @Type(type = "jsonb")
    @Column(columnDefinition = "json")
    private Map<String, Object> attributes;

    public AuditAction(long guildId) {
        this.guildId = guildId;
    }

    @Transient
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key, Class<T> type) {
        if (MapUtils.isEmpty(attributes)) {
            return null;
        }
        Object value = attributes.get(key);
        return value != null && type.isAssignableFrom(value.getClass()) ? (T) value : null;
    }
}
