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
package ru.juniperbot.common.service.impl;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.*;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.juniperbot.common.configuration.CommonProperties;
import ru.juniperbot.common.service.YouTubeService;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class YouTubeServiceImpl implements YouTubeService {

    private static final Pattern CHANNEL_URL_PATTERN = Pattern.compile("(?:(?:https|http)\\:\\/\\/)?(?:[\\w]+\\.)?youtube\\.com\\/(?:c\\/|channel\\/)?([a-zA-Z0-9\\-]{1,})");

    @Autowired
    private CommonProperties commonProperties;

    private volatile int keyCursor = 0;

    private YouTube youTube;

    private LoadingCache<String, Video> videoLoadingCache = CacheBuilder.newBuilder()
            .concurrencyLevel(7)
            .expireAfterWrite(1, TimeUnit.DAYS)
            .build(
                    new CacheLoader<>() {
                        public Video load(String token) {
                            String[] parts = token.split("/");
                            String videoId = parts[0];
                            String part = parts[1];
                            try {
                                YouTube.Videos.List list = youTube.videos().list(part);
                                list.setKey(getApiKey());
                                list.setId(videoId);
                                List<Video> items = list.execute().getItems();
                                return CollectionUtils.isNotEmpty(items) ? items.get(0) : null;
                            } catch (IOException e) {
                                log.error("Could not get video by id={}", videoId, e);
                            }
                            return null;
                        }
                    });

    @PostConstruct
    public void init() {
        try {
            youTube = new YouTube
                    .Builder(GoogleNetHttpTransport.newTrustedTransport(), JacksonFactory.getDefaultInstance(), e -> {
            })
                    .setApplicationName(YouTubeServiceImpl.class.getSimpleName())
                    .build();
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    @Override
    public List<SearchResult> search(String queryTerm, long maxResults) {
        try {
            YouTube.Search.List search = youTube.search().list("id,snippet");
            search.setKey(getApiKey());
            search.setQ(queryTerm);
            search.setType("video");
            search.setFields("items(id/videoId, snippet/title)");
            search.setMaxResults(maxResults);
            return search.execute().getItems();
        } catch (IOException e) {
            log.error("Could not perform YouTube search", e);
        }
        return Collections.emptyList();
    }

    @Override
    public List<Video> searchDetailed(String queryTerm, long maxResults) {
        try {
            List<SearchResult> results = search(queryTerm, maxResults);
            if (!results.isEmpty()) {
                YouTube.Videos.List list = youTube.videos().list("id,snippet,contentDetails");
                list.setKey(getApiKey());
                list.setId(results.stream()
                        .filter(e -> e.getId() != null && e.getId().getVideoId() != null)
                        .map(e -> e.getId().getVideoId()).collect(Collectors.joining(",")));
                return list.execute().getItems();
            }
        } catch (IOException e) {
            log.error("Could not perform YouTube search", e);
        }
        return Collections.emptyList();
    }

    @Override
    public Video getVideoById(String videoId, String part) {
        if (part == null) {
            part = "id,snippet,contentDetails";
        }
        try {
            return videoLoadingCache.get(String.format("%s/%s", videoId, part));
        } catch (ExecutionException e) {
            log.error("Could not get video by id={}", videoId, e);
        }
        return null;
    }

    @Override
    public List<SearchResult> searchChannel(String queryTerm, long maxResults) {
        try {
            SearchResult result = probeSearchByChannelUrl(queryTerm);
            if (result != null) {
                return Collections.singletonList(result);
            }
            YouTube.Search.List search = youTube.search().list("id,snippet");
            search.setKey(getApiKey());
            search.setQ(queryTerm);
            search.setType("channel");
            search.setFields("items(id/channelId, snippet/channelTitle, snippet/thumbnails/default)");
            search.setMaxResults(maxResults);
            return search.execute().getItems();
        } catch (IOException e) {
            log.error("Could not perform YouTube search", e);
        }
        return Collections.emptyList();
    }

    private SearchResult probeSearchByChannelUrl(String url) {
        if (StringUtils.isEmpty(url)) {
            return null;
        }
        Matcher matcher = CHANNEL_URL_PATTERN.matcher(url);
        if (!matcher.find()) {
            return null;
        }
        Channel channel = getChannelById(matcher.group(1));
        if (channel == null) {
            return null;
        }

        SearchResult result = new SearchResult();
        ResourceId resourceId = new ResourceId();
        resourceId.setChannelId(channel.getId());
        result.setId(resourceId);

        SearchResultSnippet snippet = new SearchResultSnippet();
        snippet.setChannelId(channel.getId());
        if (channel.getSnippet() != null) {
            snippet.setChannelTitle(channel.getSnippet().getTitle());
            snippet.setThumbnails(channel.getSnippet().getThumbnails());
        }
        result.setSnippet(snippet);
        return result;
    }

    public Channel getChannelById(String id) {
        try {
            YouTube.Channels.List list = youTube.channels().list("id,snippet");
            list.setKey(getApiKey());
            list.setId(id);
            List<Channel> channels = list.execute().getItems();
            return CollectionUtils.isNotEmpty(channels) ? channels.get(0) : null;
        } catch (IOException e) {
            log.error("Could not perform YouTube search", e);
        }

        return null;
    }

    @Override
    public String searchForUrl(String queryTerm) {
        List<SearchResult> result = search(queryTerm, 1L);
        return result.isEmpty() ? null : getUrl(result.get(0));
    }

    @Override
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

    @Override
    public String getUrl(SearchResult result) {
        return result != null && result.getId() != null
                ? getVideoUrl(result.getId().getVideoId()) : null;
    }

    private String getVideoUrl(String videoId) {
        return String.format("https://www.youtube.com/watch?v=%s", videoId);
    }

    @Override
    public String getUrl(Video result) {
        return String.format("https://www.youtube.com/watch?v=%s", result.getId());
    }

    private synchronized String getApiKey() {
        if (CollectionUtils.isEmpty(commonProperties.getYouTubeApiKeys())) {
            return null;
        }
        if (keyCursor >= commonProperties.getYouTubeApiKeys().size()) {
            keyCursor = 0;
        }
        return commonProperties.getYouTubeApiKeys().get(keyCursor++);
    }
}
