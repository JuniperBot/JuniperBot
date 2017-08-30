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

    @Column(name = "music_channel_id")
    private Long musicChannelId;

    @Column(name = "music_playlist_enabled")
    private Boolean musicPlaylistEnabled;

    @Column(name = "music_streams_enabled")
    private boolean musicStreamsEnabled;

    @Column(name = "music_user_join_enabled")
    private boolean musicUserJoinEnabled;

    @Column(name = "music_queue_limit")
    private Long musicQueueLimit;

    @Column(name = "music_duration_limit")
    private Long musicDurationLimit;

    @Column(name = "music_duplicate_limit")
    private Long musicDuplicateLimit;

    @Column(name = "is_help_private")
    private Boolean privateHelp;

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
