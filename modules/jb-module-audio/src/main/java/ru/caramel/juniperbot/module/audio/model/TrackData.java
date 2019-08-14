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
package ru.caramel.juniperbot.module.audio.model;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import ru.caramel.juniperbot.module.audio.persistence.entity.PlaylistItem;

@Getter
@Setter
public class TrackData {

    private PlaybackInstance instance;

    private PlaylistItem playlistItem;

    public static TrackData get(AudioTrack track) {
        TrackData trackData = track.getUserData(TrackData.class);
        if (trackData == null) {
            track.setUserData(trackData = new TrackData());
        }
        return trackData;
    }

    public static void setInstance(AudioTrack track, PlaybackInstance instance) {
        get(track).instance = instance;
    }

    public static void setPlaylistItem(AudioTrack track, PlaylistItem playlistItem) {
        get(track).playlistItem = playlistItem;
    }

    public static String getArtwork(AudioTrack track) {
        AudioTrackInfo info = track.getInfo();
        if (StringUtils.isNotBlank(info.getArtworkUrl())) {
            return info.getArtworkUrl();
        }
        TrackData trackData = get(track);
        if (trackData.getPlaylistItem() != null && StringUtils.isNotBlank(trackData.getPlaylistItem().getArtworkUri())) {
            return trackData.getPlaylistItem().getArtworkUri();
        }
        return null;
    }
}
