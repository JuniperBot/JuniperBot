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
package ru.juniperbot.module.audio.utils;

import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import ru.juniperbot.common.persistence.entity.Playlist;
import ru.juniperbot.common.persistence.entity.PlaylistItem;
import ru.juniperbot.common.utils.CommonUtils;
import ru.juniperbot.common.worker.utils.DiscordUtils;
import ru.juniperbot.module.audio.model.ProgressEmoji;

import java.util.Objects;

public class AudioUtils {

    private AudioUtils() {
        // private
    }

    public static PlaylistItem findPlaylist(Playlist playlist, AudioTrackInfo info) {
        info = normalizeInfo(info);
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

    public static AudioTrackInfo normalizeInfo(AudioTrackInfo info) {
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
                info.metadata);
    }

    public static String getProgressString(int progress) {
        StringBuilder builder = new StringBuilder(ProgressEmoji.START_SEGMENT.getEmoji(progress));
        progress = progress - 10;
        for (int i = 0; i < 8; i++) {
            builder.append(ProgressEmoji.INTERMEDIATE_SEGMENT.getEmoji(progress));
            progress = progress - 10;
        }
        builder.append(ProgressEmoji.END_SEGMENT.getEmoji(progress));
        return builder.toString();
    }
}
