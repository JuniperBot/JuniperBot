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
package ru.caramel.juniperbot.integration.instagram;

import org.apache.commons.collections4.CollectionUtils;
import org.jinstagram.Instagram;
import org.jinstagram.auth.model.Token;
import org.jinstagram.entity.users.feed.MediaFeed;
import org.jinstagram.entity.users.feed.MediaFeedData;
import org.jinstagram.exceptions.InstagramException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import ru.caramel.juniperbot.configuration.InstagramConfig;
import ru.caramel.juniperbot.integration.discord.DiscordClient;

import javax.annotation.PostConstruct;
import java.util.List;

@Service
public class InstagramClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiscordClient.class);

    @Autowired
    private InstagramConfig config;

    @Autowired
    private TaskScheduler scheduler;

    @Autowired
    private List<InstagramListener> listeners;

    private Instagram instagram;

    private List<MediaFeedData> cache;

    private long latestUpdate;

    @PostConstruct
    public void init() {
        if (config.getClientId() == null
                || config.getClientSecret() == null
                || config.getToken() == null
                || config.getPollUserId() == null) {
            LOGGER.warn("No Instagram clientId, clientSecret, token or userId were provided. Integration will not work");
            return;
        }
        instagram = new Instagram(config.getClientId());
        instagram.setAccessToken(new Token(config.getToken(), config.getClientSecret()));
        scheduler.scheduleWithFixedDelay(this::update, config.getUpdateInterval());
    }

    public List<MediaFeedData> getRecent() throws InstagramException {
        if (instagram == null) {
            return null;
        }
        try {
            long currentTimestamp = System.currentTimeMillis();
            if (currentTimestamp > latestUpdate + config.getTtl()) {
                synchronized (this) {
                    MediaFeed feed = instagram.getRecentMediaFeed(config.getPollUserId());
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
            if (CollectionUtils.isNotEmpty(medias)) {
                listeners.forEach(e -> e.onInstagramUpdated(medias));
            }
        } catch (InstagramException e) {
            LOGGER.error("Could not get Instagram data", e);
        }
    }
}
