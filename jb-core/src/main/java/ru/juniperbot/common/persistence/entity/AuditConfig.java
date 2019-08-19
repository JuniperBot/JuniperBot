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
package ru.juniperbot.common.persistence.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;
import ru.juniperbot.common.model.AuditActionType;
import ru.juniperbot.common.persistence.entity.base.GuildEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.List;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "audit_config")
public class AuditConfig extends GuildEntity {

    private static final long serialVersionUID = 1052650749958531237L;

    @Column(name = "enabled")
    private boolean enabled;

    @Column(name = "forward_enabled")
    private boolean forwardEnabled;

    @Column(name = "forward_channel_id")
    private String forwardChannelId;

    @Type(type = "jsonb")
    @Column(name = "forward_actions", columnDefinition = "json")
    private List<AuditActionType> forwardActions;

    public AuditConfig(long guildId) {
        this.guildId = guildId;
    }
}
