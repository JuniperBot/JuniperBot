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
package ru.juniperbot.api.subscriptions.handlers;

import com.github.twitch4j.helix.domain.User;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.juniperbot.api.dto.config.SubscriptionDto;
import ru.juniperbot.api.dto.request.SubscriptionCreateResponse;
import ru.juniperbot.api.model.SubscriptionStatus;
import ru.juniperbot.api.model.SubscriptionType;
import ru.juniperbot.api.subscriptions.integrations.TwitchSubscriptionService;
import ru.juniperbot.common.persistence.entity.TwitchConnection;

import java.util.HashMap;
import java.util.Map;

@Component
public class TwitchSubscriptionHandler extends AbstractSubscriptionHandler<TwitchConnection> {

    @Autowired
    private TwitchSubscriptionService twitchSubscriptionService;

    @Override
    public SubscriptionDto getSubscription(TwitchConnection connection) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("twitch.id", connection.getUserId());
        attributes.put("twitch.login", connection.getLogin());
        attributes.put("twitch.description", connection.getDescription());
        attributes.put("twitch.announce", connection.getAnnounceMessage());
        attributes.put("twitch.sendEmbed", connection.isSendEmbed());
        SubscriptionDto dto = getDtoForHook(connection.getGuildId(), connection.getWebHook());
        dto.setId(connection.getId());
        dto.setAttributes(attributes);
        dto.setType(SubscriptionType.TWITCH);
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
        String login = getValue(data, "login", String.class);
        if (StringUtils.isEmpty(login)) {
            throw new IllegalArgumentException("Wrong data");
        }

        User user = twitchSubscriptionService.getUser(login);
        if (user == null) {
            return getFailedCreatedDto("wrong_user");
        }
        TwitchConnection connection = twitchSubscriptionService.create(guildId, user);
        return getCreatedDto(getSubscription(connection));
    }

    @Override
    @Transactional
    public boolean update(SubscriptionDto subscription) {
        TwitchConnection connection = twitchSubscriptionService.find(subscription.getId());
        if (!check(connection)) {
            return false;
        }
        updateWebHook(connection, subscription);

        String announce = getValue(subscription.getAttributes(), "twitch.announce", String.class);
        connection.setAnnounceMessage(announce);

        Boolean sendEmbed = getValue(subscription.getAttributes(), "twitch.sendEmbed", Boolean.class);
        connection.setSendEmbed(Boolean.TRUE.equals(sendEmbed));

        twitchSubscriptionService.save(connection);
        return true;
    }

    @Override
    @Transactional
    public void delete(long id) {
        TwitchConnection connection = twitchSubscriptionService.find(id);
        if (check(connection)) {
            twitchSubscriptionService.delete(connection);
        }
    }

    @Override
    public Class<TwitchConnection> getEntityType() {
        return TwitchConnection.class;
    }

    @Override
    public SubscriptionType getType() {
        return SubscriptionType.TWITCH;
    }
}
