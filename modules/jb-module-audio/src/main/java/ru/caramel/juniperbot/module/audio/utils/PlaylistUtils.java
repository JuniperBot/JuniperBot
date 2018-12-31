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
package ru.caramel.juniperbot.module.audio.utils;

import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import ru.caramel.juniperbot.core.utils.CommonUtils;
import ru.caramel.juniperbot.core.utils.DiscordUtils;
import ru.caramel.juniperbot.module.audio.persistence.entity.Playlist;
import ru.caramel.juniperbot.module.audio.persistence.entity.PlaylistItem;

import java.util.Objects;

public class PlaylistUtils {

    private PlaylistUtils() {
        // private
    }

    public static PlaylistItem find(Playlist playlist, AudioTrackInfo info) {
        info = getNormalized(info);
        for (PlaylistItem item : playlist.getItems()) {
            if (item != null &&
                    Objects.equals(item.getTitle(), info.title) &&
                    Objects.equals(item.getAuthor(), info.author) &&
                    Objects.equals(item.getLength(), info.length) &&
                    Objects.equals(item.getIdentifier(), info.identifier) &&
                    Objects.equals(item.getUri(), info.uri)) {
                return item;
            }
        }
        return null;
    }

    public static AudioTrackInfo getNormalized(AudioTrackInfo info) {
        if (info == null) {
            return null;
        }
        return new AudioTrackInfo(
                CommonUtils.trimTo(info.title, 255),
                CommonUtils.trimTo(info.author, 255),
                info.length,
                CommonUtils.trimTo(info.identifier, 1000),
                info.isStream,
                CommonUtils.trimTo(DiscordUtils.getUrl(info.uri), 1000),
                CommonUtils.trimTo(DiscordUtils.getUrl(info.artworkUri), 1000));
    }
}
