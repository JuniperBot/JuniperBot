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
package ru.caramel.juniperbot.persistence.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ru.caramel.juniperbot.persistence.entity.base.BaseEntity;

import javax.persistence.*;
import javax.validation.constraints.Size;

@Entity
@Table(name = "welcome_message")
@ToString
@Getter
@Setter
public class WelcomeMessage extends BaseEntity {
    private static final long serialVersionUID = -3872054410668142206L;

    @OneToOne(cascade = {CascadeType.DETACH, CascadeType.REFRESH}, fetch = FetchType.LAZY)
    @JoinColumn(name = "guild_config_id")
    private GuildConfig config;

    @Column(name = "join_enabled")
    private boolean joinEnabled;

    @Column(name = "join_rich_enabled")
    private boolean joinRichEnabled;

    @Column(name = "join_message")
    @Size(max = 1800)
    private String joinMessage;

    @Column(name = "join_channel_id")
    private Long joinChannelId;

    @Column(name = "join_to_dm")
    private boolean joinToDM;

    @Column(name = "leave_enabled")
    private boolean leaveEnabled;

    @Column(name = "leave_rich_enabled")
    private boolean leaveRichEnabled;

    @Column(name = "leave_message")
    @Size(max = 1800)
    private String leaveMessage;

    @Column(name = "leave_channel_id")
    private Long leaveChannelId;

}
