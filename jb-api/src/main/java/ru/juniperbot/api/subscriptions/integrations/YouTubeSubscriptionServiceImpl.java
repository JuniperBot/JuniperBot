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
package ru.juniperbot.api.subscriptions.integrations;

import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import club.minnced.discord.webhook.send.WebhookMessage;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import com.google.api.services.youtube.model.Channel;
import com.google.api.services.youtube.model.ChannelSnippet;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoSnippet;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
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
import ru.juniperbot.api.ApiProperties;
import ru.juniperbot.common.configuration.CommonProperties;
import ru.juniperbot.common.persistence.entity.YouTubeChannel;
import ru.juniperbot.common.persistence.entity.YouTubeConnection;
import ru.juniperbot.common.persistence.repository.YouTubeChannelRepository;
import ru.juniperbot.common.persistence.repository.YouTubeConnectionRepository;
import ru.juniperbot.common.service.ConfigService;
import ru.juniperbot.common.service.YouTubeService;
import ru.juniperbot.common.support.MapPlaceholderResolver;
import ru.juniperbot.common.utils.CommonUtils;

import java.awt.*;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
public class YouTubeSubscriptionServiceImpl extends BaseSubscriptionService<YouTubeConnection, Video, Channel> implements YouTubeSubscriptionService {

    private static final String PUSH_ENDPOINT = "https://pubsubhubbub.appspot.com/subscribe";

    private static final String CHANNEL_RSS_ENDPOINT = "https://www.youtube.com/xml/feeds/videos.xml?channel_id=";

    @Autowired
    private CommonProperties commonProperties;

    @Autowired
    private ApiProperties apiProperties;

    @Autowired
    private YouTubeChannelRepository channelRepository;

    @Autowired
    private ConfigService configService;

    @Autowired
    private YouTubeService youTubeService;

    private YouTubeConnectionRepository repository;

    private final RestTemplate restTemplate = new RestTemplate(CommonUtils.createRequestFactory());

    private final Cache<String, String> videoCache = CacheBuilder.newBuilder()
            .concurrencyLevel(7)
            .expireAfterWrite(7, TimeUnit.DAYS)
            .build();

    public YouTubeSubscriptionServiceImpl(@Autowired YouTubeConnectionRepository repository) {
        super(repository);
        this.repository = repository;
    }

    @Override
    public Channel getUser(String userName) {
        return null;
    }

    private String getVideoUrl(String videoId) {
        return String.format("https://www.youtube.com/watch?v=%s", videoId);
    }

    private String getChannelUrl(String channelId) {
        return String.format("https://www.youtube.com/channel/%s", channelId);
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
            Video video = youTubeService.getVideoById(videoId, "id,snippet");
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
            announce = getMessage(connection, "discord.youtube.announce");
        }
        String content = PLACEHOLDER.replacePlaceholders(announce, resolver);

        WebhookMessageBuilder builder = new WebhookMessageBuilder()
                .setContent(content);

        if (connection.isSendEmbed()) {
            WebhookEmbedBuilder embedBuilder = new WebhookEmbedBuilder();
            VideoSnippet snippet = video.getSnippet();
            embedBuilder.setAuthor(new WebhookEmbed.EmbedAuthor(snippet.getChannelTitle(),
                    connection.getIconUrl(),
                    getChannelUrl(snippet.getChannelId())));

            if (snippet.getThumbnails() != null && snippet.getThumbnails().getMedium() != null) {
                embedBuilder.setImageUrl(snippet.getThumbnails().getMedium().getUrl());
            }
            embedBuilder.setDescription(CommonUtils.mdLink(snippet.getTitle(), getVideoUrl(video.getId())));
            embedBuilder.setColor(Color.RED.getRGB());
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
                commonProperties.getBranding().getWebsiteUrl(),
                apiProperties.getYouTube().getPubSubSecret(),
                CommonUtils.urlEncode(channel.getChannelId())));
        map.add("hub.topic", CHANNEL_RSS_ENDPOINT + channel.getChannelId());
        map.add("hub.mode", "subscribe");
        map.add("hub.verify", "async");
        map.add("hub.verify_token", apiProperties.getYouTube().getPubSubSecret());
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(PUSH_ENDPOINT, request, String.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("Could not subscribe to " + channel.getChannelId());
        }
    }

    @Override
    @Transactional
    public void prolongChannel(String channelId) {
        YouTubeChannel channel = channelRepository.findByChannelId(channelId);
        if (channel != null) {
            channel.setExpiresAt(DateTime.now().plusDays(4).toDate());
            channelRepository.save(channel);
        }
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
    @Transactional(readOnly = true)
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
        long threshold = channels.size() * (apiProperties.getYouTube().getResubscribeThresholdPct() / 100);
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
                subscribe(connection);
                successful.incrementAndGet();
            } catch (Exception e) {
                log.warn("Could not resubscribe channelId={}", channels, e);
                if (failed.incrementAndGet() >= thresholdFinal) {
                    log.warn("YouTubeConnection resubscription threshold reached {} with successful {}", thresholdFinal,
                            successful.longValue(), e);
                }
            }
        });

        log.info("Finished YouTube channels resubscription in {} ms with successful {} of total {}",
                System.currentTimeMillis() - start, successful, channels.size());
    }
}
