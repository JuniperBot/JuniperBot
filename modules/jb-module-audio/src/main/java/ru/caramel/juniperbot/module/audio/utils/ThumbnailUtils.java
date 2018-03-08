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
import ru.caramel.juniperbot.module.audio.persistence.entity.PlaylistItem;

import java.net.URI;
import java.net.URISyntaxException;

public final class ThumbnailUtils {

    private ThumbnailUtils() {
        // helper class
    }

    public static String getThumbnail(PlaylistItem item) {
        return getThumbnail(item.getUri(), item.getIdentifier());
    }

    public static String getThumbnail(AudioTrackInfo item) {
        return getThumbnail(item.uri, item.identifier);
    }

    public static String getThumbnail(String url, String identifier) {
        try {
            URI uri = new URI(url);
            if (uri.getHost().contains("youtube.com") || uri.getHost().contains("youtu.be")) {
                return String.format("https://img.youtube.com/vi/%s/0.jpg", identifier);
            }
        } catch (URISyntaxException e) {
            // fall down
        }
        return null;
    }
}
