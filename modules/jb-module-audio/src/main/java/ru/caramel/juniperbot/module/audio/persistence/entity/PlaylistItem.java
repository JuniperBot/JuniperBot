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

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.caramel.juniperbot.core.persistence.entity.LocalMember;
import ru.caramel.juniperbot.core.persistence.entity.base.BaseEntity;
import ru.caramel.juniperbot.core.utils.CommonUtils;

import javax.persistence.*;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "playlist_item")
public class PlaylistItem extends BaseEntity {

    private static final long serialVersionUID = -3967389800974743538L;

    @ManyToOne(cascade = {CascadeType.REFRESH, CascadeType.DETACH, CascadeType.MERGE, CascadeType.PERSIST})
    @JoinColumn(name="playlist_id")
    public Playlist playlist;

    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.DETACH})
    @JoinColumn(name = "requested_by_id")
    private LocalMember requestedBy;

    @Column
    private String title;

    @Column
    private String author;

    @Column(length = 1000)
    private String identifier;

    @Column(length = 1000)
    private String uri;

    @Column
    private long length;

    @Column(name = "is_stream")
    private boolean stream;

    @Column
    private int index;

    @Column
    private String type;

    @Column(name = "artwork_url")
    private String artworkUri;

    public PlaylistItem(AudioTrack track, LocalMember requestedBy) {
        this.requestedBy = requestedBy;
        type = track.getClass().getSimpleName();
        AudioTrackInfo info = track.getInfo();
        this.title = CommonUtils.trimTo(info.title, 255);
        this.author = CommonUtils.trimTo(info.author, 255);
        this.identifier = CommonUtils.trimTo(info.identifier, 1000);
        this.uri = CommonUtils.trimTo(info.uri, 1000);
        this.length = info.length;
        this.stream = info.isStream;
        this.artworkUri = info.artworkUri;
    }
}
