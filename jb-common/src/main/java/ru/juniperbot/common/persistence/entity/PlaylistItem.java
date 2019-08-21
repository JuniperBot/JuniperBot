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
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;
import ru.juniperbot.common.persistence.entity.base.BaseEntity;

import javax.persistence.*;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "playlist_item")
public class PlaylistItem extends BaseEntity {

    private static final long serialVersionUID = -3967389800974743538L;

    @ManyToOne(cascade = {CascadeType.REFRESH, CascadeType.DETACH, CascadeType.MERGE, CascadeType.PERSIST})
    @JoinColumn(name = "playlist_id")
    public Playlist playlist;

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.DETACH})
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

    @Type(type = "org.hibernate.type.BinaryType")
    @Column
    private byte[] data;

    public PlaylistItem(LocalMember requestedBy) {
        this.requestedBy = requestedBy;
    }
}
