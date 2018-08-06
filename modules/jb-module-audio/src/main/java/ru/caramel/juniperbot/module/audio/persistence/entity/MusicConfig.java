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
package ru.caramel.juniperbot.module.audio.persistence.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;
import ru.caramel.juniperbot.core.persistence.entity.base.GuildEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.List;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "music_config")
public class MusicConfig extends GuildEntity {
    private static final long serialVersionUID = 7052650749958531237L;

    @Column(name = "channel_id")
    private Long channelId;

    @Column(name = "text_channel_id")
    private Long textChannelId;

    @Column(name = "playlist_enabled")
    private Boolean playlistEnabled;

    @Column(name = "auto_play")
    private String autoPlay;

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

    @Column(name = "voice_volume")
    private int voiceVolume;

    @Type(type = "jsonb")
    @Column(columnDefinition = "json")
    private List<Long> roles;

    public MusicConfig(long guildId) {
        this.guildId = guildId;
    }
}
