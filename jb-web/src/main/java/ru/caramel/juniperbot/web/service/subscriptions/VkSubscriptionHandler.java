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

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.caramel.juniperbot.core.persistence.entity.GuildConfig;
import ru.caramel.juniperbot.core.utils.CommonUtils;
import ru.caramel.juniperbot.module.vk.model.VkConnectionStatus;
import ru.caramel.juniperbot.module.vk.persistence.entity.VkConnection;
import ru.caramel.juniperbot.module.vk.service.VkService;
import ru.caramel.juniperbot.web.dto.api.config.SubscriptionDto;
import ru.caramel.juniperbot.web.model.SubscriptionStatus;
import ru.caramel.juniperbot.web.model.SubscriptionType;

import java.util.HashMap;
import java.util.Map;

@Component
public class VkSubscriptionHandler extends AbstractSubscriptionHandler<VkConnection> {

    @Autowired
    private VkService vkService;

    @Override
    public SubscriptionDto getSubscription(VkConnection connection) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("attachments", CommonUtils.reverse(vkService.getAttachmentTypes(), connection.getAttachments()));
        SubscriptionDto dto = getDtoForHook(connection.getConfig().getGuildId(), connection.getWebHook());
        dto.setId(connection.getId());
        dto.setAttributes(attributes);
        dto.setType(SubscriptionType.VK);
        dto.setStatus(VkConnectionStatus.CONNECTED == connection.getStatus()
                ? SubscriptionStatus.ACTIVE : SubscriptionStatus.PENDING);
        return dto;
    }

    @Override
    public SubscriptionDto create(long fuildId, Map<String, ?> data) {
        GuildConfig config = configService.getOrCreate(fuildId);
        String name = getValue(data, "name", String.class);
        String code = getValue(data, "code", String.class);
        if (StringUtils.isEmpty(name) || StringUtils.isEmpty(code)) {
            throw new IllegalArgumentException("Wrong data");
        }
        VkConnection connection = vkService.create(config, name, code);
        return getSubscription(connection);
    }

    @Override
    public void update(SubscriptionDto subscription) {

    }

    @Override
    public void delete(long id) {

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
