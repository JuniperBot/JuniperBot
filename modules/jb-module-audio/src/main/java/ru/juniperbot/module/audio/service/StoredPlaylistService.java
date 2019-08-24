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
package ru.juniperbot.module.audio.service;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import ru.juniperbot.common.persistence.entity.Playlist;
import ru.juniperbot.common.persistence.entity.PlaylistItem;
import ru.juniperbot.module.audio.model.PlaybackInstance;
import ru.juniperbot.module.audio.model.TrackRequest;

import java.util.List;

public interface StoredPlaylistService {

    void storeToPlaylist(PlaybackInstance instance, List<TrackRequest> requests);

    Playlist getPlaylist(PlaybackInstance instance);

    void refreshStoredPlaylist(Playlist playlist, List<AudioTrack> tracks);

    void refreshStoredPlaylist(PlaybackInstance instance);

    PlaylistItem save(PlaylistItem playlistItem);
}
