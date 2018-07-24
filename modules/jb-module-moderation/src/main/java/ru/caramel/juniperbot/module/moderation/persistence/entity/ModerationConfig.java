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
package ru.caramel.juniperbot.module.moderation.persistence.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;
import ru.caramel.juniperbot.core.persistence.entity.GuildOwnedEntity;
import ru.caramel.juniperbot.module.moderation.model.WarnExceedAction;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.List;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "mod_config")
public class ModerationConfig extends GuildOwnedEntity {

    private static final long serialVersionUID = 7052650749958531237L;

    public static final int DEFAULT_MAX_WARNINGS = 3;

    public static final int DEFAULT_MUTE_COUNT = 1440;

    @Type(type = "jsonb")
    @Column(columnDefinition = "json")
    private List<Long> roles;

    @Column(name = "public_colors")
    private boolean publicColors;

    @Column(name = "max_warnings")
    private int maxWarnings = DEFAULT_MAX_WARNINGS;

    @Column(name = "warn_exceed_action")
    @Enumerated(EnumType.STRING)
    @NotNull
    private WarnExceedAction warnExceedAction = WarnExceedAction.BAN;

    @Column(name = "mute_count")
    private int muteCount = DEFAULT_MUTE_COUNT;

}
