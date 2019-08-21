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
package ru.juniperbot.api.subscriptions.handlers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.juniperbot.api.dto.config.SubscriptionDto;
import ru.juniperbot.api.dto.request.SubscriptionCreateResponse;
import ru.juniperbot.api.model.SubscriptionStatus;
import ru.juniperbot.api.model.SubscriptionType;
import ru.juniperbot.api.subscriptions.integrations.InstagramService;
import ru.juniperbot.common.model.exception.AccessDeniedException;
import ru.juniperbot.common.persistence.entity.JuniPost;
import ru.juniperbot.common.service.JuniPostService;

import java.util.Map;

@Component
public class JuniSubscriptionHandler extends AbstractSubscriptionHandler<JuniPost> {

    @Autowired
    private JuniPostService juniPostService;

    @Autowired
    private InstagramService instagramService;

    @Override
    public SubscriptionDto getSubscription(JuniPost juniPost) {
        SubscriptionDto dto = getDtoForHook(juniPost.getGuildId(), juniPost.getWebHook());
        dto.setId(juniPost.getId());
        dto.setName(instagramService.getAccountName());
        dto.setIconUrl(instagramService.getIconUrl());
        dto.setStatus(SubscriptionStatus.ACTIVE);
        dto.setType(SubscriptionType.JUNIPERFOXX);
        return dto;
    }

    @Override
    @Transactional
    public boolean update(SubscriptionDto subscription) {
        JuniPost post = juniPostService.get(subscription.getId());
        if (!check(post)) {
            return false;
        }
        updateWebHook(post, subscription);
        juniPostService.save(post);
        return true;
    }

    @Override
    public void delete(long id) {
        throw new AccessDeniedException();
    }

    @Override
    public SubscriptionCreateResponse create(long fuildId, Map<String, ?> data) {
        throw new AccessDeniedException();
    }

    @Override
    public Class<JuniPost> getEntityType() {
        return JuniPost.class;
    }

    @Override
    public SubscriptionType getType() {
        return SubscriptionType.JUNIPERFOXX;
    }
}
