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
package ru.caramel.juniperbot.modules.ranking.persistence.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;
import ru.caramel.juniperbot.core.persistence.entity.GuildConfig;
import ru.caramel.juniperbot.core.persistence.entity.base.BaseEntity;
import ru.caramel.juniperbot.modules.ranking.model.Reward;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.util.List;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "ranking_config")
public class RankingConfig extends BaseEntity {
    private static final long serialVersionUID = -2380208935931616881L;

    @OneToOne(mappedBy = "rankingConfig", fetch = FetchType.LAZY)
    private GuildConfig guildConfig;

    @Column
    private boolean enabled;

    @Column(name = "announcement_enabled")
    private boolean announcementEnabled;

    @Column(name = "is_whisper")
    private boolean whisper;

    @Column(name = "reset_on_leave")
    private boolean resetOnLeave;

    @Column
    @Size(max = 1800)
    private String announcement;

    @Column(name = "banned_roles", columnDefinition = "text[]")
    @Type(type = "string-array")
    private String[] bannedRoles;

    @Type(type = "jsonb")
    @Column(columnDefinition = "json")
    private List<Reward> rewards;

}
