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
package ru.juniperbot.module.audio.model;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.BasicAudioPlaylist;
import lombok.Getter;
import lombok.Setter;
import ru.juniperbot.common.persistence.entity.Playlist;

import java.util.List;

@Getter
@Setter
public class StoredPlaylist extends BasicAudioPlaylist {

    private long guildId;

    private long playlistId;

    private String playlistUuid;

    /**
     * @param playlist Playlist
     * @param tracks   List of tracks in the playlist
     */
    public StoredPlaylist(Playlist playlist, List<AudioTrack> tracks) {
        super(playlist.getUuid(), tracks, null, false);
        this.playlistId = playlist.getId();
        this.playlistUuid = playlist.getUuid();
        this.guildId = playlist.getGuildId();
    }
}
