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
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;
import org.hibernate.validator.constraints.NotEmpty;
import ru.caramel.juniperbot.persistence.entity.base.BaseEntity;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.util.List;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "guild_config")
@NamedEntityGraphs({
        @NamedEntityGraph(name = GuildConfig.COMMANDS_GRAPH, attributeNodes = @NamedAttributeNode("commands"))
})
@NamedQuery(name = GuildConfig.FIND_BY_GUILD_ID, query = "SELECT g FROM GuildConfig g WHERE g.guildId = :guildId")
public class GuildConfig extends BaseEntity {

    private static final long serialVersionUID = 1599157155969887890L;

    public static final String FIND_BY_GUILD_ID = "GuildConfig.findByGuildId";

    public static final String COMMANDS_GRAPH = "GuildConfig.commandsGraph";

    @Column(name = "guild_id")
    private long guildId;

    @Basic
    @NotEmpty
    @Size(max = 20)
    private String prefix;

    @Column(name = "disabled_commands", columnDefinition = "text[]")
    @Type(type = "string-array")
    private String[] disabledCommands;

    @Column(name = "is_help_private")
    private Boolean privateHelp;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "music_config_id")
    private MusicConfig musicConfig;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "web_hook_id")
    private WebHook webHook;

    @OneToMany(mappedBy = "config", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<VkConnection> vkConnections;

    @OneToMany(mappedBy = "config", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CustomCommand> commands;

    public GuildConfig(long guildId) {
        this.guildId = guildId;
    }
}
