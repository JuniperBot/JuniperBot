package ru.caramel.juniperbot.persistence.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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
public class GuildConfig extends BaseEntity {

    private static final long serialVersionUID = 1599157155969887890L;

    @Column(name = "guild_id")
    private long guildId;

    @Basic
    @NotEmpty
    @Size(max = 20)
    private String prefix;

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
