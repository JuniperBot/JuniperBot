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
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.webhook.WebhookMessage;
import net.dv8tion.jda.webhook.WebhookMessageBuilder;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import ru.caramel.juniperbot.core.common.service.BrandingService;
import ru.caramel.juniperbot.core.common.service.EmergencyService;
import ru.caramel.juniperbot.core.message.resolver.MapPlaceholderResolver;
import ru.caramel.juniperbot.core.subscription.service.BaseSubscriptionService;
import ru.caramel.juniperbot.core.utils.CommonUtils;
import ru.caramel.juniperbot.module.social.persistence.entity.YouTubeChannel;
import ru.caramel.juniperbot.module.social.persistence.entity.YouTubeConnection;
import ru.caramel.juniperbot.module.social.persistence.repository.YouTubeChannelRepository;
import ru.caramel.juniperbot.module.social.persistence.repository.YouTubeConnectionRepository;
import ru.caramel.juniperbot.module.social.service.YouTubeService;

import javax.annotation.PostConstruct;
import java.awt.*;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Slf4j
@Service
public class YouTubeServiceImpl extends BaseSubscriptionService<YouTubeConnection, Video, Channel> implements YouTubeService {

    private static final String PUSH_ENDPOINT = "https://pubsubhubbub.appspot.com/subscribe";

    private static final String CHANNEL_RSS_ENDPOINT = "https://www.youtube.com/xml/feeds/videos.xml?channel_id=";

    @Value("${integrations.youTube.apiKey}")
    private String[] apiKeys;

    private volatile int keyCursor = 0;

    @Getter
    @Value("${integrations.youTube.pubSubSecret}")
    private String pubSubSecret;

    @Getter
    @Value("${integrations.youTube.resubscribeThresholdPct:10}")
    private Long resubscribeThresholdPct;

    @Autowired
    private BrandingService brandingService;

    @Autowired
    private EmergencyService emergencyService;

    @Autowired
    private YouTubeChannelRepository channelRepository;

    private YouTube youTube;

    private YouTubeConnectionRepository repository;

    private final RestTemplate restTemplate = new RestTemplate(CommonUtils.createRequestFactory());

    private final Cache<String, String> videoCache = CacheBuilder.newBuilder()
            .concurrencyLevel(7)
            .expireAfterWrite(7, TimeUnit.DAYS)
            .build();

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

    private String getChannelUrl(String channelId) {
        return String.format("https://www.youtube.com/channel/%s", channelId);
    }

    @Override
    public String getUrl(Video result) {
        return String.format("https://www.youtube.com/watch?v=%s", result.getId());
    }

    @Override
    @Transactional
    public void notifyVideo(String channelId, String videoId) {
        synchronized (videoCache) {
            if (videoCache.getIfPresent(videoId) != null) {
                return; // do not notify this video again
            }
            videoCache.put(videoId, videoId);
        }

        try {
            Video video = getVideoById(videoId, "id,snippet");
            if (video == null) {
                log.error("No suitable video found for id={}", videoId);
                return;
            }

            if (video.getSnippet() != null && video.getSnippet().getPublishedAt() != null) {
                var publishedAt = video.getSnippet().getPublishedAt();
                LocalDate dateTime = new DateTime(publishedAt.getValue()).toLocalDate();
                if (Days.daysBetween(dateTime, LocalDate.now()).getDays() >= 1) {
                    return;
                }
            }
            repository
                    .findActiveConnections(channelId)
                    .forEach(e -> notifyConnection(video, e));
        } catch (Exception e) {
            videoCache.invalidate(videoId);
            throw e;
        }
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
        subscribe(connection.getChannel());
        return connection;
    }

    @Override
    @Transactional
    public void subscribe(YouTubeChannel channel) {
        LocalDate now = LocalDate.now();
        LocalDate expiredAt = channel.getExpiresAt() != null
                ? LocalDate.fromDateFields(channel.getExpiresAt())
                : LocalDate.now();
        if (now.isBefore(expiredAt)) {
            return;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("hub.callback", String.format("%s/api/public/youtube/callback/publish?secret=%s&channel=%s",
                brandingService.getWebHost(), pubSubSecret, CommonUtils.urlEncode(channel.getChannelId())));
        map.add("hub.topic", CHANNEL_RSS_ENDPOINT + channel.getChannelId());
        map.add("hub.mode", "subscribe");
        map.add("hub.verify", "async");
        map.add("hub.verify_token", pubSubSecret);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(PUSH_ENDPOINT, request, String.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("Could not subscribe to " + channel.getChannelId());
        }
        channel.setExpiresAt(DateTime.now().plusDays(7).toDate());
        channelRepository.save(channel);
    }

    @Override
    protected YouTubeConnection createConnection(Channel channel) {
        YouTubeConnection connection = new YouTubeConnection();
        connection.setChannel(getOrCreateChannel(channel.getId()));
        ChannelSnippet snippet = channel.getSnippet();
        connection.setName(CommonUtils.trimTo(snippet.getTitle(), 255));
        connection.setDescription(CommonUtils.trimTo(snippet.getDescription(), 255));
        if (snippet.getThumbnails() != null && snippet.getThumbnails().getDefault() != null) {
            connection.setIconUrl(snippet.getThumbnails().getDefault().getUrl());
        }
        return connection;
    }

    private YouTubeChannel getOrCreateChannel(String channelId) {
        YouTubeChannel channel = channelRepository.findByChannelId(channelId);
        if (channel != null) {
            return channel;
        }
        channel = new YouTubeChannel();
        channel.setChannelId(channelId);
        return channelRepository.save(channel);
    }

    /**
     * Appspot PubSubHubBub subscriptions expires each 10 days so we should resubscribe all
     */
    @Override
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public synchronized void resubscribeAll() {
        log.info("Starting YouTube resubscription.");

        Date currentDate = new Date();
        List<YouTubeChannel> channels = repository.findToResubscribe(currentDate);
        if (CollectionUtils.isEmpty(channels)) {
            log.info("Nothing to resubscribe.");
            return;
        }
        AtomicLong failed = new AtomicLong();
        AtomicLong successful = new AtomicLong();
        long threshold = channels.size() * (resubscribeThresholdPct / 100);
        if (threshold == 0) {
            threshold = 1;
        }

        log.info("Starting YouTube resubscription with total {} channels and {} threshold",
                channels.size(), threshold);

        final long start = System.currentTimeMillis();
        final long thresholdFinal = threshold;
        channels.parallelStream().forEach(connection -> {
            if (failed.longValue() >= thresholdFinal) {
                return;
            }
            try {
                contextService.inTransaction(() -> subscribe(connection));
                successful.incrementAndGet();
            } catch (Exception e) {
                log.warn("Could not resubscribe channelId={}", channels, e);
                if (failed.incrementAndGet() >= thresholdFinal) {
                    emergencyService.error(String.format("YouTubeConnection resubscription threshold reached %s with " +
                            "successful %s", thresholdFinal, successful.longValue()), e);
                }
            }
        });

        log.info("Finished YouTube channels resubscription in {} ms with successful {} of total {}",
                System.currentTimeMillis() - start, successful, channels.size());
    }

    private synchronized String getApiKey() {
        if (apiKeys == null || apiKeys.length == 0) {
            return null;
        }
        if (keyCursor >= apiKeys.length) {
            keyCursor = 0;
        }
        return apiKeys[keyCursor++];
    }
}
