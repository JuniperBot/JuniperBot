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
package ru.caramel.juniperbot.module.social.service.impl;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.*;
import lombok.Getter;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.webhook.WebhookMessage;
import net.dv8tion.jda.webhook.WebhookMessageBuilder;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import ru.caramel.juniperbot.core.service.BrandingService;
import ru.caramel.juniperbot.core.service.impl.BaseSubscriptionService;
import ru.caramel.juniperbot.core.utils.CommonUtils;
import ru.caramel.juniperbot.core.utils.MapPlaceholderResolver;
import ru.caramel.juniperbot.module.social.persistence.entity.YouTubeConnection;
import ru.caramel.juniperbot.module.social.persistence.repository.YouTubeConnectionRepository;
import ru.caramel.juniperbot.module.social.service.YouTubeService;

import javax.annotation.PostConstruct;
import java.awt.*;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class YouTubeServiceImpl extends BaseSubscriptionService<YouTubeConnection, Video, Channel> implements YouTubeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(YouTubeServiceImpl.class);

    private static final String PUSH_ENDPOINT = "https://pubsubhubbub.appspot.com/subscribe";

    private static final String CHANNEL_RSS_ENDPOINT = "https://www.youtube.com/xml/feeds/videos.xml?channel_id=";

    @Value("${integrations.youTube.apiKey}")
    private String apiKey;

    @Getter
    @Value("${integrations.youTube.pubSubSecret}")
    private String pubSubSecret;

    @Autowired
    private BrandingService brandingService;

    private YouTube youTube;

    private YouTubeConnectionRepository repository;

    private RestTemplate restTemplate = new RestTemplate();

    public YouTubeServiceImpl(@Autowired YouTubeConnectionRepository repository) {
        super(repository);
        this.repository = repository;
    }

    @PostConstruct
    public void init() {
        try {
            youTube = new YouTube
                    .Builder(GoogleNetHttpTransport.newTrustedTransport(), JacksonFactory.getDefaultInstance(), e -> {})
                    .setApplicationName(YouTubeServiceImpl.class.getSimpleName())
                    .build();
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    @Override
    public Channel getUser(String userName) {
        return null;
    }

    @Override
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

    @Override
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

    @Override
    public Video getVideoById(String videoId, String part) {
        try {
            if (part == null) {
                part = "id,snippet,contentDetails";
            }
            YouTube.Videos.List list = youTube.videos().list(part);
            list.setKey(apiKey);
            list.setId(videoId);
            List<Video> items = list.execute().getItems();
            return CollectionUtils.isNotEmpty(items) ? items.get(0) : null;
        } catch (IOException e) {
            LOGGER.error("Could not get video by id={}", videoId, e);
        }
        return null;
    }

    @Override
    public List<SearchResult> searchChannel(String queryTerm, long maxResults) {
        try {
            YouTube.Search.List search = youTube.search().list("id,snippet");
            search.setKey(apiKey);
            search.setQ(queryTerm);
            search.setType("channel");
            search.setFields("items(id/channelId, snippet/channelTitle, snippet/thumbnails/default)");
            search.setMaxResults(maxResults);
            return search.execute().getItems();
        } catch (IOException e) {
            LOGGER.error("Could not perform YouTube search", e);
        }
        return Collections.emptyList();
    }

    public Channel getChannelById(String id) {
        try {
            YouTube.Channels.List list = youTube.channels().list("id,snippet");
            list.setKey(apiKey);
            list.setId(id);
            List<Channel> channels = list.execute().getItems();
            return CollectionUtils.isNotEmpty(channels) ? channels.get(0) : null;
        } catch (IOException e) {
            LOGGER.error("Could not perform YouTube search", e);
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

    public String getVideoUrl(String videoId) {
        return String.format("https://www.youtube.com/watch?v=%s", videoId);
    }

    public String getChannelUrl(String channelId) {
        return String.format("https://www.youtube.com/channel/%s", channelId);
    }

    @Override
    public String getUrl(Video result) {
        return String.format("https://www.youtube.com/watch?v=%s", result.getId());
    }

    @Override
    public void notifyVideo(String channelId, String videoId) {
        List<YouTubeConnection> connections = repository.findActiveConnections(channelId);
        if (CollectionUtils.isEmpty(connections)) {
            return;
        }
        Video video = getVideoById(videoId, "id,snippet");
        if (video == null) {
            LOGGER.error("No suitable video found for id={}", videoId);
            return;
        }
        connections.forEach(e -> notifyConnection(video, e));
    }

    @Override
    protected WebhookMessage createMessage(Video video, YouTubeConnection connection) {
        MapPlaceholderResolver resolver = new MapPlaceholderResolver();
        resolver.put("channel", video.getSnippet().getChannelTitle());
        resolver.put("video", video.getSnippet().getTitle());
        resolver.put("link", getVideoUrl(video.getId()));
        String announce = connection.getAnnounceMessage();
        if (StringUtils.isBlank(announce)) {
            announce = messageService.getMessage("discord.youtube.announce");
        }
        String content = PLACEHOLDER.replacePlaceholders(announce, resolver);

        WebhookMessageBuilder builder = new WebhookMessageBuilder()
                .setContent(content);

        if (connection.isSendEmbed()) {
            EmbedBuilder embedBuilder = messageService.getBaseEmbed();
            VideoSnippet snippet = video.getSnippet();
            embedBuilder.setAuthor(snippet.getChannelTitle(),
                    getChannelUrl(snippet.getChannelId()), connection.getIconUrl());

            if (snippet.getThumbnails() != null && snippet.getThumbnails().getMedium() != null) {
                embedBuilder.setImage(snippet.getThumbnails().getMedium().getUrl());
            }
            embedBuilder.setDescription(CommonUtils.mdLink(snippet.getTitle(), getVideoUrl(video.getId())));
            embedBuilder.setColor(Color.RED);
            if (snippet.getPublishedAt() != null) {
                embedBuilder.setTimestamp(Instant.ofEpochMilli(snippet.getPublishedAt().getValue()));
            }

            builder.addEmbeds(embedBuilder.build());
        }
        return builder.build();
    }

    @Override
    @Transactional
    public YouTubeConnection create(long guildId, Channel channel) {
        YouTubeConnection connection = super.create(guildId, channel);
        subscribe(connection);
        return connection;
    }

    private void subscribe(YouTubeConnection connection) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("hub.callback", String.format("%s/api/public/youtube/callback/publish?secret=%s", brandingService.getWebHost(), pubSubSecret));
        map.add("hub.topic", CHANNEL_RSS_ENDPOINT + connection.getChannelId());
        map.add("hub.mode", "subscribe");
        map.add("hub.verify", "async");
        map.add("hub.verify_token", pubSubSecret);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(PUSH_ENDPOINT, request, String.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("Could not subscribe to " + connection.getChannelId());
        }
    }

    @Override
    protected YouTubeConnection createConnection(Channel channel) {
        YouTubeConnection connection = new YouTubeConnection();
        connection.setChannelId(channel.getId());
        ChannelSnippet snippet = channel.getSnippet();
        connection.setName(CommonUtils.trimTo(snippet.getTitle(), 255));
        connection.setDescription(CommonUtils.trimTo(snippet.getDescription(), 255));
        if (snippet.getThumbnails() != null && snippet.getThumbnails().getDefault() != null) {
            connection.setIconUrl(snippet.getThumbnails().getDefault().getUrl());
        }
        return connection;
    }
}
