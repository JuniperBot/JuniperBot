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

import com.google.common.collect.Lists;
import me.philippheuer.twitch4j.TwitchClient;
import me.philippheuer.twitch4j.TwitchClientBuilder;
import me.philippheuer.twitch4j.model.Channel;
import me.philippheuer.twitch4j.model.Stream;
import me.philippheuer.twitch4j.model.User;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.webhook.WebhookMessage;
import net.dv8tion.jda.webhook.WebhookMessageBuilder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.caramel.juniperbot.core.service.impl.BaseSubscriptionService;
import ru.caramel.juniperbot.core.utils.CommonUtils;
import ru.caramel.juniperbot.core.utils.MapPlaceholderResolver;
import ru.caramel.juniperbot.module.social.persistence.entity.TwitchConnection;
import ru.caramel.juniperbot.module.social.persistence.repository.TwitchConnectionRepository;
import ru.caramel.juniperbot.module.social.service.TwitchService;

import javax.annotation.PostConstruct;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TwitchServiceImpl extends BaseSubscriptionService<TwitchConnection, Stream, User> implements TwitchService {

    private static final Logger log = LoggerFactory.getLogger(TwitchServiceImpl.class);

    private static final Color TWITCH_COLOR = CommonUtils.hex2Rgb("64439A");

    @Value("${integrations.twitch.clientId:}")
    private String clientId;

    @Value("${integrations.twitch.secret:}")
    private String secret;

    @Value("${integrations.twitch.oauthKey:}")
    private String oauthKey;

    @Value("${integrations.twitch.updateInterval:}")
    private Long updateInterval;

    private TwitchClient client;

    private Set<Long> liveStreamsCache = Collections.synchronizedSet(new HashSet<>());

    private TwitchConnectionRepository repository;

    public TwitchServiceImpl(@Autowired TwitchConnectionRepository repository) {
        super(repository);
        this.repository = repository;
    }

    @PostConstruct
    public void init() {
        if (StringUtils.isEmpty(clientId) || StringUtils.isEmpty(secret)) {
            log.warn("No valid Twitch credentials provided");
            return;
        }
        client = TwitchClientBuilder.init()
                .withClientId(clientId)
                .withClientSecret(secret)
                .withCredential(oauthKey)
                .connect();
        schedule(updateInterval);
    }

    @Override
    protected synchronized void update() {
        if (client == null) {
            return;
        }
        log.debug("Twitch channels notification finished...");

        List<Channel> channels = repository.findChannelIds().stream().map(e -> {
            Channel channel = new Channel();
            channel.setId(e);
            return channel;
        }).collect(Collectors.toList());

        if (channels.isEmpty()) {
            return;
        }

        Set<Stream> liveStreams = new HashSet<>(channels.size());

        Lists.partition(channels, 100).forEach(e -> liveStreams.addAll(client.getStreamEndpoint()
                .getLiveStreams(channels, null, null, null, 100, 0)));

        if (liveStreams.isEmpty()) {
            liveStreamsCache.clear();
            return;
        }

        List<Stream> streamsToNotify = liveStreams.stream()
                .filter(e -> !liveStreamsCache.contains(e.getId()))
                .collect(Collectors.toList());

        Lists.partition(streamsToNotify, 1000).forEach(e -> {
            try {
                List<TwitchConnection> toSave = new ArrayList<>(e.size());
                Map<Long, Stream> streamMap = e.stream().collect(Collectors.toMap(s -> s.getChannel().getId(),
                        Function.identity()));
                repository.findActiveConnections(e.stream()
                        .map(s -> s.getChannel().getId())
                        .collect(Collectors.toSet()))
                        .forEach(c -> {
                            Stream stream = streamMap.get(c.getUserId());
                            if (stream != null) {
                                if (updateIfRequired(stream.getChannel(), c)) {
                                    toSave.add(c);
                                }
                                notifyConnection(stream, c);
                            }
                });
                repository.saveAll(toSave);
            } catch (Exception ex) {
                log.warn("Could not notify twitch partition", ex);
            }
        });

        liveStreamsCache.clear();
        liveStreamsCache.addAll(liveStreams.stream().map(Stream::getId).collect(Collectors.toSet()));
        log.debug("Twitch channels notification finished...");
    }

    private boolean updateIfRequired(Channel channel, TwitchConnection connection) {
        boolean updateRequired = false;
        if (!Objects.equals(channel.getLogo(), connection.getIconUrl())) {
            connection.setIconUrl(channel.getLogo());
            updateRequired = true;
        }
        if (!Objects.equals(channel.getName(), connection.getLogin())) {
            connection.setLogin(channel.getName());
            updateRequired = true;
        }
        if (!Objects.equals(channel.getDisplayName(), connection.getName())) {
            connection.setName(channel.getDisplayName());
            updateRequired = true;
        }
        return updateRequired;
    }

    @Override
    protected WebhookMessage createMessage(Stream stream, TwitchConnection connection) {
        MapPlaceholderResolver resolver = new MapPlaceholderResolver();
        resolver.put("streamer", stream.getChannel().getDisplayName());
        resolver.put("link", stream.getChannel().getUrl());
        resolver.put("game", stream.getGame());
        String announce = connection.getAnnounceMessage();
        if (StringUtils.isBlank(announce)) {
            announce = messageService.getMessage("discord.twitch.announce");
        }
        String content = PLACEHOLDER.replacePlaceholders(announce, resolver);

        WebhookMessageBuilder builder = new WebhookMessageBuilder()
                .setContent(content);

        if (connection.isSendEmbed()) {
            Channel channel = stream.getChannel();

            EmbedBuilder embedBuilder = messageService.getBaseEmbed();
            embedBuilder.setAuthor(CommonUtils.trimTo(channel.getDisplayName(), MessageEmbed.TITLE_MAX_LENGTH),
                    channel.getUrl(), channel.getLogo());
            embedBuilder.setThumbnail(channel.getLogo());
            embedBuilder.setDescription(CommonUtils.trimTo(channel.getStatus(), MessageEmbed.TITLE_MAX_LENGTH));
            embedBuilder.setColor(TWITCH_COLOR);

            if (stream.getPreview() != null && stream.getPreview().getMedium() != null) {
                embedBuilder.setImage(stream.getPreview().getMedium());
            }
            embedBuilder.addField(messageService.getMessage("discord.viewers.title"),
                    CommonUtils.formatNumber(stream.getViewers()), false);
            if (StringUtils.isNotBlank(stream.getGame())) {
                embedBuilder.addField(messageService.getMessage("discord.game.title"),
                        CommonUtils.trimTo(stream.getGame(), MessageEmbed.VALUE_MAX_LENGTH), false);
            }
            builder.addEmbeds(embedBuilder.build());
        }
        return builder.build();
    }

    @Override
    protected TwitchConnection createConnection(User user) {
        TwitchConnection connection = new TwitchConnection();
        connection.setUserId(user.getId());
        connection.setLogin(user.getName());
        connection.setName(user.getDisplayName());
        connection.setDescription(user.getBio());
        connection.setIconUrl(user.getLogo());
        connection.setSendEmbed(true);
        return connection;
    }

    @Override
    public User getUser(String userName) {
        return client != null ? client.getUserEndpoint().getUserByUserName(userName) : null;
    }
}
