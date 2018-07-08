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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.caramel.juniperbot.core.model.exception.AccessDeniedException;
import ru.caramel.juniperbot.module.junipost.persistence.entity.JuniPost;
import ru.caramel.juniperbot.module.junipost.persistence.repository.JuniPostRepository;
import ru.caramel.juniperbot.web.dto.api.config.SubscriptionDto;
import ru.caramel.juniperbot.web.model.SubscriptionStatus;
import ru.caramel.juniperbot.web.model.SubscriptionType;

import java.util.Map;

@Component
public class JuniSubscriptionHandler extends AbstractSubscriptionHandler<JuniPost> {

    @Override
    public SubscriptionDto getSubscription(JuniPost juniPost) {
        SubscriptionDto dto = getDtoForHook(juniPost.getGuildConfig().getGuildId(), juniPost.getWebHook());
        dto.setId(juniPost.getId());
        dto.setStatus(SubscriptionStatus.ACTIVE);
        dto.setType(SubscriptionType.JUNIPERFOXX);
        return dto;
    }

    @Override
    public void update(SubscriptionDto subscription) {

    }

    @Override
    public void delete(long id) {
        throw new AccessDeniedException();
    }

    @Override
    public SubscriptionDto create(long fuildId, Map<String, ?> data) {
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
