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
package ru.caramel.juniperbot.module.custom.persistence.entity;

import lombok.Getter;
import lombok.Setter;
import ru.caramel.juniperbot.core.persistence.entity.GuildConfig;
import ru.caramel.juniperbot.core.persistence.entity.base.BaseEntity;
import ru.caramel.juniperbot.module.custom.model.CommandType;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Getter
@Setter
@Entity
@Table(name = "custom_command")
public class CustomCommand extends BaseEntity {

    private static final long serialVersionUID = -8582315203089732918L;

    @ManyToOne(cascade = {CascadeType.DETACH, CascadeType.REFRESH}, fetch = FetchType.LAZY)
    @JoinColumn(name = "guild_config_id")
    private GuildConfig config;

    @Column
    @Enumerated(EnumType.STRING)
    @NotNull
    private CommandType type;

    @Size(min = 1, max = 25)
    @NotNull
    private String key;

    @Column(columnDefinition = "text")
    @NotNull
    private String content;
}
