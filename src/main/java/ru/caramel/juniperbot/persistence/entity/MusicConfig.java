package ru.caramel.juniperbot.persistence.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.caramel.juniperbot.persistence.entity.base.BaseEntity;

import javax.persistence.*;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "music_config")
public class MusicConfig extends BaseEntity {
    private static final long serialVersionUID = 7052650749958531237L;

    @Column(name = "channel_id")
    private Long channelId;

    @Column(name = "playlist_enabled")
    private Boolean playlistEnabled;

    @Column(name = "streams_enabled")
    private boolean streamsEnabled;

    @Column(name = "user_join_enabled")
    private boolean userJoinEnabled;

    @Column(name = "queue_limit")
    private Long queueLimit;

    @Column(name = "duration_limit")
    private Long durationLimit;

    @Column(name = "duplicate_limit")
    private Long duplicateLimit;

}
