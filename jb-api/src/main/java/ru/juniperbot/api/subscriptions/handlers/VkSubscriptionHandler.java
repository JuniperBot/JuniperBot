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

import com.vk.api.sdk.objects.wall.WallpostAttachmentType;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.juniperbot.api.dto.config.SubscriptionDto;
import ru.juniperbot.api.dto.request.SubscriptionCreateResponse;
import ru.juniperbot.api.model.SubscriptionStatus;
import ru.juniperbot.api.model.SubscriptionType;
import ru.juniperbot.api.model.VkInfo;
import ru.juniperbot.api.subscriptions.integrations.VkSubscriptionService;
import ru.juniperbot.common.model.VkConnectionStatus;
import ru.juniperbot.common.persistence.entity.VkConnection;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class VkSubscriptionHandler extends AbstractSubscriptionHandler<VkConnection> {

    @Autowired
    private VkSubscriptionService vkSubscriptionService;

    @Override
    public SubscriptionDto getSubscription(VkConnection connection) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("attachments", reverse(connection.getAttachmentTypes()));
        attributes.put("vk.token", connection.getToken());
        attributes.put("vk.groupId", connection.getGroupId());
        attributes.put("vk.groupOnlyPosts", connection.isGroupOnlyPosts());
        attributes.put("vk.showPostLink", connection.isShowPostLink());
        attributes.put("vk.showDate", connection.isShowDate());
        attributes.put("vk.mentionEveryone", connection.isMentionEveryone());
        SubscriptionDto dto = getDtoForHook(connection.getGuildId(), connection.getWebHook());
        dto.setId(connection.getId());
        dto.setAttributes(attributes);
        dto.setType(SubscriptionType.VK);
        if (StringUtils.isEmpty(dto.getName())) {
            dto.setName(connection.getName());
        }
        dto.setStatus(VkConnectionStatus.CONNECTED == connection.getStatus()
                ? SubscriptionStatus.ACTIVE : SubscriptionStatus.PENDING);
        return dto;
    }

    @Override
    public SubscriptionCreateResponse create(long guildId, Map<String, ?> data) {
        String name = getValue(data, "name", String.class);
        String code = getValue(data, "code", String.class);
        if (StringUtils.isEmpty(name) || StringUtils.isEmpty(code)) {
            throw new IllegalArgumentException("Wrong data");
        }
        VkConnection connection = vkSubscriptionService.create(guildId, new VkInfo(name, code));
        return getCreatedDto(getSubscription(connection));
    }

    private List<String> reverse(Collection values) {
        return vkSubscriptionService.getAttachmentTypes().stream()
                .map(WallpostAttachmentType::name)
                .filter(e -> values == null || !values.contains(e))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public boolean update(SubscriptionDto subscription) {
        VkConnection connection = vkSubscriptionService.find(subscription.getId());
        if (!check(connection)) {
            return false;
        }
        if (VkConnectionStatus.CONNECTED.equals(connection.getStatus())) {
            updateWebHook(connection, subscription);
        } else {
            connection.getWebHook().setEnabled(subscription.isEnabled());
        }
        Collection attachments = getValue(subscription.getAttributes(), "attachments", Collection.class);
        if (attachments == null) {
            attachments = Collections.emptyList();
        }
        connection.setAttachmentTypes(reverse(attachments));

        Boolean groupOnlyPosts = getValue(subscription.getAttributes(), "vk.groupOnlyPosts", Boolean.class);
        connection.setGroupOnlyPosts(groupOnlyPosts != null ? groupOnlyPosts : false);

        Boolean showPostLink = getValue(subscription.getAttributes(), "vk.showPostLink", Boolean.class);
        connection.setShowPostLink(showPostLink != null ? showPostLink : true);

        Boolean showDate = getValue(subscription.getAttributes(), "vk.showDate", Boolean.class);
        connection.setShowDate(showDate != null ? showDate : true);

        Boolean mentionEveryone = getValue(subscription.getAttributes(), "vk.mentionEveryone", Boolean.class);
        connection.setMentionEveryone(mentionEveryone != null ? mentionEveryone : false);
        vkSubscriptionService.save(connection);
        return true;
    }

    @Override
    @Transactional
    public void delete(long id) {
        VkConnection connection = vkSubscriptionService.find(id);
        if (check(connection)) {
            vkSubscriptionService.delete(connection);
        }
    }

    @Override
    public Class<VkConnection> getEntityType() {
        return VkConnection.class;
    }

    @Override
    public SubscriptionType getType() {
        return SubscriptionType.VK;
    }
}
