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
package ru.juniperbot.common.service;

import com.google.api.services.youtube.model.Channel;
import com.google.api.services.youtube.model.SearchResult;
import com.google.api.services.youtube.model.Video;
import ru.juniperbot.common.persistence.entity.YouTubeChannel;
import ru.juniperbot.common.persistence.entity.YouTubeConnection;

import java.util.List;

public interface YouTubeService {

    List<SearchResult> search(String queryTerm, long maxResults);

    List<Video> searchDetailed(String queryTerm, long maxResults);

    List<SearchResult> searchChannel(String queryTerm, long maxResults);

    Channel getChannelById(String id);

    Video getVideoById(String videoId, String part);

    String searchForUrl(String queryTerm);

    Long extractTimecode(String input);

    String getUrl(SearchResult result);

    String getUrl(Video result);
}
