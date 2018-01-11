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
package ru.caramel.juniperbot.core.modules.junipost.service;

import org.jinstagram.Instagram;
import org.jinstagram.auth.model.Token;
import org.jinstagram.entity.users.feed.MediaFeed;
import org.jinstagram.entity.users.feed.MediaFeedData;
import org.jinstagram.exceptions.InstagramException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;

@Service
public class InstagramService {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstagramService.class);

    @Value("${instagram.clientId}")
    private String clientId;

    @Value("${instagram.clientSecret}")
    private String clientSecret;

    @Value("${instagram.token}")
    private String token;

    @Value("${instagram.pollUserId}")
    private String pollUserId;

    @Value("${instagram.ttl:30000}")
    private Long ttl;

    @Value("${instagram.updateInterval:30000}")
    private Long updateInterval;

    @Autowired
    private TaskScheduler scheduler;

    @Autowired
    private PostService postService;

    private Instagram instagram;

    private List<MediaFeedData> cache;

    private long latestUpdate;

    @PostConstruct
    public void init() {
        if (clientId == null
                || clientSecret == null
                || token == null
                || pollUserId == null) {
            LOGGER.warn("No Instagram clientId, clientSecret, token or userId were provided. Integration will not work");
            return;
        }
        instagram = new Instagram(clientId);
        instagram.setAccessToken(new Token(token, clientSecret));
        scheduler.scheduleWithFixedDelay(this::update, updateInterval);
    }

    public List<MediaFeedData> getRecent() throws InstagramException {
        if (instagram == null) {
            return null;
        }
        try {
            long currentTimestamp = System.currentTimeMillis();
            if (currentTimestamp > latestUpdate + ttl) {
                synchronized (this) {
                    MediaFeed feed = instagram.getRecentMediaFeed(pollUserId);
                    cache = feed.getData();
                    latestUpdate = currentTimestamp;
                }
            }
            return cache;
        } catch (InstagramException e) {
            LOGGER.error("Could not get Instagram data", e);
        }
        return null;
    }

    private void update() {
        try {
            List<MediaFeedData> medias = getRecent();
            postService.onInstagramUpdated(medias);
        } catch (InstagramException e) {
            LOGGER.error("Could not get Instagram data", e);
        }
    }
}
