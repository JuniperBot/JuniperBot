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

import com.vk.api.sdk.objects.wall.WallpostAttachmentType;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.caramel.juniperbot.core.utils.CommonUtils;
import ru.caramel.juniperbot.module.social.model.VkConnectionStatus;
import ru.caramel.juniperbot.module.social.persistence.entity.VkConnection;
import ru.caramel.juniperbot.module.social.service.VkService;
import ru.caramel.juniperbot.web.dto.config.SubscriptionDto;
import ru.caramel.juniperbot.web.dto.request.SubscriptionCreateResponse;
import ru.caramel.juniperbot.web.model.SubscriptionStatus;
import ru.caramel.juniperbot.web.model.SubscriptionType;

import java.util.*;

@Component
public class VkSubscriptionHandler extends AbstractSubscriptionHandler<VkConnection> {

    @Autowired
    private VkService vkService;

    @Override
    public SubscriptionDto getSubscription(VkConnection connection) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("attachments", CommonUtils.reverse(vkService.getAttachmentTypes(), connection.getAttachments()));
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
        VkConnection connection = vkService.create(guildId, name, code);
        return getCreatedDto(getSubscription(connection));
    }

    @Override
    @Transactional
    public boolean update(SubscriptionDto subscription) {
        VkConnection connection = vkService.find(subscription.getId());
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
        Set<WallpostAttachmentType> attachmentTypes = CommonUtils.safeEnumSet(attachments, WallpostAttachmentType.class);
        connection.setAttachments(CommonUtils.reverse(vkService.getAttachmentTypes(), attachmentTypes));

        Boolean groupOnlyPosts = getValue(subscription.getAttributes(), "vk.groupOnlyPosts", Boolean.class);
        connection.setGroupOnlyPosts(groupOnlyPosts != null ? groupOnlyPosts : false);

        Boolean showPostLink = getValue(subscription.getAttributes(), "vk.showPostLink", Boolean.class);
        connection.setShowPostLink(showPostLink != null ? showPostLink : true);

        Boolean showDate = getValue(subscription.getAttributes(), "vk.showDate", Boolean.class);
        connection.setShowDate(showDate != null ? showDate : true);

        Boolean mentionEveryone = getValue(subscription.getAttributes(), "vk.mentionEveryone", Boolean.class);
        connection.setMentionEveryone(mentionEveryone != null ? mentionEveryone : false);
        vkService.save(connection);
        return true;
    }

    @Override
    @Transactional
    public void delete(long id) {
        VkConnection connection = vkService.find(id);
        if (check(connection)) {
            vkService.delete(connection);
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
