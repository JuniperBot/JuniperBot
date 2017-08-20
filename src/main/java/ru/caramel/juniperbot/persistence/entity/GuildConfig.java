package ru.caramel.juniperbot.persistence.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.validator.constraints.NotEmpty;
import ru.caramel.juniperbot.persistence.entity.base.BaseEntity;

import javax.persistence.*;
import javax.validation.constraints.Size;

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

    @Column(name = "music_queue_limit")
    private Long musicQueueLimit;

    @Column(name = "music_duration_limit")
    private Long musicDurationLimit;

    @Column(name = "music_duplicate_limit")
    private Long musicDuplicateLimit;

    @Column(name = "is_help_private")
    private Boolean privateHelp;

    public GuildConfig(long guildId) {
        this.guildId = guildId;
    }
}
