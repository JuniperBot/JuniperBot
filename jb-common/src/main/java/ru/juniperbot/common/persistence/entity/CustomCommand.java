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
import lombok.Setter;
import org.hibernate.annotations.Type;
import ru.juniperbot.common.model.CommandType;
import ru.juniperbot.common.persistence.entity.base.GuildEntity;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "custom_command")
public class CustomCommand extends GuildEntity {

    private static final long serialVersionUID = -8582315203089732918L;

    @Size(min = 1, max = 25)
    @NotNull
    private String key;

    @Column(columnDefinition = "text")
    @NotNull
    private String content;

    @OneToOne(cascade = {CascadeType.ALL}, fetch = FetchType.EAGER)
    @JoinColumn(name = "command_config_id")
    private CommandConfig commandConfig;

    @Column
    @Enumerated(EnumType.STRING)
    @NotNull
    private CommandType type;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "message_template_id")
    private MessageTemplate messageTemplate;

    @Type(type = "jsonb")
    @Column(name = "roles_to_add", columnDefinition = "json")
    private List<Long> rolesToAdd;

    @Type(type = "jsonb")
    @Column(name = "roles_to_remove", columnDefinition = "json")
    private List<Long> rolesToRemove;

}
