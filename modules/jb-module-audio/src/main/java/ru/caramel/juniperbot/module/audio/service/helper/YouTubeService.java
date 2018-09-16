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
package ru.caramel.juniperbot.module.audio.service.helper;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchResult;
import com.google.api.services.youtube.model.Video;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class YouTubeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(YouTubeService.class);

    @Value("${integrations.youTube.apiKey}")
    private String apiKey;

    private YouTube youTube;

    @PostConstruct
    public void init() {
        try {
            youTube = new YouTube
                    .Builder(GoogleNetHttpTransport.newTrustedTransport(), JacksonFactory.getDefaultInstance(), e -> {})
                    .setApplicationName(YouTubeService.class.getSimpleName())
                    .build();
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public List<SearchResult> search(String queryTerm, long maxResults) {
        try {
            YouTube.Search.List search = youTube.search().list("id,snippet");
            search.setKey(apiKey);
            search.setQ(queryTerm);
            search.setType("video");
            search.setFields("items(id/videoId, snippet/title)");
            search.setMaxResults(maxResults);
            return search.execute().getItems();
        } catch (IOException e) {
            LOGGER.error("Could not perform YouTube search", e);
        }
        return Collections.emptyList();
    }

    public List<Video> searchDetailed(String queryTerm, long maxResults) {
        try {
            List<SearchResult> results = search(queryTerm, maxResults);
            YouTube.Videos.List list = youTube.videos().list("id,snippet,contentDetails");
            list.setKey(apiKey);
            list.setId(results.stream()
                    .filter(e -> e.getId() != null && e.getId().getVideoId() != null)
                    .map(e -> e.getId().getVideoId()).collect(Collectors.joining(",")));
            return list.execute().getItems();
        } catch (IOException e) {
            LOGGER.error("Could not perform YouTube search", e);
        }
        return Collections.emptyList();
    }

    public String searchForUrl(String queryTerm) {
        List<SearchResult> result = search(queryTerm, 1L);
        return result.isEmpty() ? null : getUrl(result.get(0));
    }

    public Long extractTimecode(String input) {
        try {
            URIBuilder uri = new URIBuilder(input);
            if (!uri.getHost().endsWith("youtube.com") && !uri.getHost().endsWith("youtu.be")) {
                return null;
            }
            String timecode = uri.getQueryParams().stream()
                    .filter(e -> "t".equals(e.getName()))
                    .map(NameValuePair::getValue)
                    .findFirst().orElse(null);
            if (StringUtils.isNotEmpty(timecode)) {
                if (StringUtils.isNumeric(timecode)) {
                    return Long.parseLong(timecode) * 1000;
                }
                return Duration.parse("PT" + timecode).toMillis();
            }
        } catch (Exception e) {
            // impossible
        }
        return null;
    }

    public String getUrl(SearchResult result) {
        return result != null && result.getId() != null
                ? String.format("https://www.youtube.com/watch?v=%s", result.getId().getVideoId()) : null;
    }

    public String getUrl(Video result) {
        return String.format("https://www.youtube.com/watch?v=%s", result.getId());
    }
}
