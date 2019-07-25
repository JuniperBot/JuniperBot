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
import org.hibernate.annotations.Type;
import ru.caramel.juniperbot.core.common.persistence.base.BaseEntity;
import ru.caramel.juniperbot.core.moderation.model.ModerationActionType;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.List;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "mod_action")
public class ModerationAction extends BaseEntity {

    private static final long serialVersionUID = 7673157017326710653L;

    @ManyToOne(cascade = {CascadeType.REFRESH, CascadeType.DETACH, CascadeType.MERGE, CascadeType.PERSIST})
    @JoinColumn(name = "config_id")
    public ModerationConfig config;

    @Enumerated(EnumType.STRING)
    @NotNull
    private ModerationActionType type;

    @Column
    private int count;

    @Column
    private int duration;

    @Type(type = "jsonb")
    @Column(name = "assign_roles", columnDefinition = "json")
    private List<Long> assignRoles;

    @Type(type = "jsonb")
    @Column(name = "revoke_roles", columnDefinition = "json")
    private List<Long> revokeRoles;

}
