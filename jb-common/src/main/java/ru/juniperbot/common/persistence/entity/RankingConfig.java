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
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;
import ru.juniperbot.common.model.RankingReward;
import ru.juniperbot.common.persistence.entity.base.GuildEntity;

import javax.persistence.*;
import java.util.List;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "ranking_config")
public class RankingConfig extends GuildEntity {
    private static final long serialVersionUID = -2380208935931616881L;

    @Column
    private boolean enabled;

    @Column
    private boolean voiceEnabled;

    @Column(name = "announcement_enabled")
    private boolean announcementEnabled;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "announce_template_id")
    private MessageTemplate announceTemplate;

    @Column(name = "reset_on_leave")
    private boolean resetOnLeave;

    @Column(precision = 3, scale = 2)
    private double textExpMultiplier = 1.0d;

    @Column(precision = 3, scale = 2)
    private double voiceExpMultiplier = 1.0d;

    @Column
    private Integer maxVoiceMembers;

    @Column(name = "banned_roles", columnDefinition = "text[]")
    @Type(type = "string-array")
    private String[] bannedRoles;

    @Type(type = "jsonb")
    @Column(columnDefinition = "json")
    private List<RankingReward> rewards;

    @Type(type = "jsonb")
    @Column(name = "ignored_channels", columnDefinition = "json")
    private List<Long> ignoredChannels;

    @Type(type = "jsonb")
    @Column(name = "ignored_voice_channels", columnDefinition = "json")
    private List<Long> ignoredVoiceChannels;

    @Column(name = "cookie_enabled")
    private boolean cookieEnabled;

    public RankingConfig(long guildId) {
        this.guildId = guildId;
    }
}
