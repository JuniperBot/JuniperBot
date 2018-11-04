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
import ru.caramel.juniperbot.core.persistence.entity.base.GuildEntity;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "guild_config")
public class GuildConfig extends GuildEntity {

    private static final long serialVersionUID = 1599157155969887890L;

    @Basic
    @Size(max = 100)
    private String name;

    @Basic
    @Size(max = 7)
    private String color;

    @Column(name = "icon_url")
    private String iconUrl;

    @Basic
    @NotEmpty
    @Size(max = 20)
    private String prefix;

    @Column(name = "is_help_private")
    private Boolean privateHelp;

    @Basic
    @NotEmpty
    @Size(max = 10)
    private String locale;

    @NotEmpty
    @Size(max = 10)
    @Column(name = "command_locale")
    private String commandLocale;

    @Column(name = "time_zone")
    private String timeZone;

    public GuildConfig(long guildId) {
        this.guildId = guildId;
    }
}
