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
package ru.juniperbot.api.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.juniperbot.api.dto.config.SubscriptionDto;
import ru.juniperbot.api.dto.request.SubscriptionCreateRequest;
import ru.juniperbot.api.dto.request.SubscriptionCreateResponse;
import ru.juniperbot.api.model.SubscriptionType;
import ru.juniperbot.api.subscriptions.handlers.SubscriptionHandler;
import ru.juniperbot.common.persistence.entity.VkConnection;
import ru.juniperbot.common.persistence.entity.TwitchConnection;
import ru.juniperbot.common.persistence.entity.YouTubeConnection;
import ru.juniperbot.common.persistence.repository.TwitchConnectionRepository;
import ru.juniperbot.common.persistence.repository.VkConnectionRepository;
import ru.juniperbot.common.persistence.repository.YouTubeConnectionRepository;
import ru.juniperbot.common.service.JuniPostService;

import java.util.*;

@Service
public class SubscriptionDao extends AbstractDao {

    @Autowired
    private VkConnectionRepository vkConnectionRepository;

    @Autowired
    private TwitchConnectionRepository twitchConnectionRepository;

    @Autowired
    private JuniPostService juniPostService;

    @Autowired
    private YouTubeConnectionRepository youTubeConnectionRepository;

    private Map<Class<?>, SubscriptionHandler<?>> handlersByClass = new HashMap<>();

    private Map<SubscriptionType, SubscriptionHandler<?>> handlersByType = new HashMap<>();

    @Transactional
    public List<SubscriptionDto> getSubscriptions(long guildId) {
        List<SubscriptionDto> result = new ArrayList<>();

        SubscriptionDto dto = getSubscription(juniPostService.getOrCreate(guildId));
        if (dto != null) {
            result.add(dto);
        }

        List<VkConnection> vkConnections = vkConnectionRepository.findAllByGuildId(guildId);
        vkConnections.stream().map(this::getSubscription).filter(Objects::nonNull).forEach(result::add);

        List<TwitchConnection> twitchConnections = twitchConnectionRepository.findAllByGuildId(guildId);
        twitchConnections.stream().map(this::getSubscription).filter(Objects::nonNull).forEach(result::add);

        List<YouTubeConnection> youTubeConnections = youTubeConnectionRepository.findAllByGuildId(guildId);
        youTubeConnections.stream().map(this::getSubscription).filter(Objects::nonNull).forEach(result::add);
        return result;
    }

    @Transactional
    public SubscriptionCreateResponse create(long guildId, SubscriptionCreateRequest request) {
        SubscriptionHandler<?> handler = handlersByType.get(request.getType());
        if (handler != null) {
            return handler.create(guildId, request.getData());
        }
        return null;
    }

    @Transactional
    public boolean update(SubscriptionDto dto) {
        if (dto.getId() == null) {
            return false;
        }
        SubscriptionHandler<?> handler = handlersByType.get(dto.getType());
        return handler != null && handler.update(dto);
    }

    @Transactional
    public boolean delete(SubscriptionType type, long id) {
        SubscriptionHandler<?> handler = handlersByType.get(type);
        if (handler != null) {
            handler.delete(id);
            return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private <T> SubscriptionDto getSubscription(T object) {
        SubscriptionHandler<T> handler = (SubscriptionHandler<T>) handlersByClass.get(object.getClass());
        return handler != null ? handler.getSubscription(object) : null;
    }

    @Autowired
    public void setSubscriptionHandlers(List<SubscriptionHandler<?>> handlers) {
        handlers.forEach(e -> {
            handlersByClass.put(e.getEntityType(), e);
            handlersByType.put(e.getType(), e);
        });
    }
}
