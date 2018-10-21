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
package ru.caramel.juniperbot.web.service.subscriptions;

import com.google.api.services.youtube.model.Channel;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.caramel.juniperbot.module.social.persistence.entity.YouTubeConnection;
import ru.caramel.juniperbot.module.social.service.YouTubeService;
import ru.caramel.juniperbot.web.dto.config.SubscriptionDto;
import ru.caramel.juniperbot.web.dto.request.SubscriptionCreateResponse;
import ru.caramel.juniperbot.web.model.SubscriptionStatus;
import ru.caramel.juniperbot.web.model.SubscriptionType;

import java.util.HashMap;
import java.util.Map;

@Component
public class YouTubeSubscriptionHandler extends AbstractSubscriptionHandler<YouTubeConnection> {

    @Autowired
    private YouTubeService youTubeService;

    @Override
    public SubscriptionDto getSubscription(YouTubeConnection connection) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("youtube.channelId", connection.getChannelId());
        attributes.put("youtube.description", connection.getDescription());
        attributes.put("youtube.announce", connection.getAnnounceMessage());
        attributes.put("youtube.sendEmbed", connection.isSendEmbed());
        SubscriptionDto dto = getDtoForHook(connection.getGuildId(), connection.getWebHook());
        dto.setId(connection.getId());
        dto.setAttributes(attributes);
        dto.setType(SubscriptionType.YOUTUBE);
        if (StringUtils.isEmpty(dto.getName())) {
            dto.setName(connection.getName());
        }
        if (StringUtils.isEmpty(dto.getIconUrl())) {
            dto.setIconUrl(connection.getIconUrl());
        }
        dto.setStatus(SubscriptionStatus.ACTIVE);
        return dto;
    }

    @Override
    public SubscriptionCreateResponse create(long guildId, Map<String, ?> data) {
        String channelId = getValue(data, "id", String.class);
        if (StringUtils.isEmpty(channelId)) {
            throw new IllegalArgumentException("Wrong data");
        }

        Channel channel = youTubeService.getChannelById(channelId);
        if (channel == null) {
            return getFailedCreatedDto("wrong_channel");
        }
        YouTubeConnection connection = youTubeService.create(guildId, channel);
        return getCreatedDto(getSubscription(connection));
    }

    @Override
    @Transactional
    public boolean update(SubscriptionDto subscription) {
        YouTubeConnection connection = youTubeService.find(subscription.getId());
        if (!check(connection)) {
            return false;
        }
        updateWebHook(connection, subscription);

        String announce = getValue(subscription.getAttributes(), "youtube.announce", String.class);
        connection.setAnnounceMessage(announce);

        Boolean sendEmbed = getValue(subscription.getAttributes(), "youtube.sendEmbed", Boolean.class);
        connection.setSendEmbed(Boolean.TRUE.equals(sendEmbed));

        youTubeService.save(connection);
        return true;
    }

    @Override
    @Transactional
    public void delete(long id) {
        YouTubeConnection connection = youTubeService.find(id);
        if (check(connection)) {
            youTubeService.delete(connection);
        }
    }

    @Override
    public Class<YouTubeConnection> getEntityType() {
        return YouTubeConnection.class;
    }

    @Override
    public SubscriptionType getType() {
        return SubscriptionType.YOUTUBE;
    }
}
