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

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Webhook;
import org.springframework.beans.factory.annotation.Autowired;
import ru.caramel.juniperbot.core.model.exception.AccessDeniedException;
import ru.caramel.juniperbot.core.persistence.entity.WebHook;
import ru.caramel.juniperbot.core.persistence.entity.WebHookOwnedEntity;
import ru.caramel.juniperbot.core.persistence.repository.WebHookRepository;
import ru.caramel.juniperbot.core.service.ConfigService;
import ru.caramel.juniperbot.core.service.DiscordService;
import ru.caramel.juniperbot.core.service.WebHookService;
import ru.caramel.juniperbot.web.dto.api.config.SubscriptionDto;
import ru.caramel.juniperbot.web.security.auth.DiscordTokenServices;

import java.util.Map;

public abstract class AbstractSubscriptionHandler<T> implements SubscriptionHandler<T> {

    @Autowired
    protected ConfigService configService;

    @Autowired
    protected WebHookService webHookService;

    @Autowired
    protected DiscordService discordService;

    @Autowired
    protected DiscordTokenServices tokenServices;

    @Autowired
    protected WebHookRepository webHookRepository;

    protected boolean check(WebHookOwnedEntity entity) {
        if (entity == null) {
            return false;
        }
        long guildId = entity.getGuildConfig().getGuildId();
        if (!tokenServices.hasPermission(guildId)) {
            throw new AccessDeniedException();
        }
        return true;
    }

    protected SubscriptionDto getDtoForHook(long guildId, WebHook webHook) {
        SubscriptionDto dto = new SubscriptionDto();
        dto.setEnabled(webHook.isEnabled());

        if (discordService.isConnected(guildId)) {
            Guild guild = discordService.getShardManager().getGuildById(guildId);
            if (guild != null && guild.getSelfMember().hasPermission(Permission.MANAGE_WEBHOOKS)) {
                dto.setAvailable(true);
                Webhook webhook = webHookService.getWebHook(guild, webHook);
                if (webhook != null) {
                    if (webhook.getDefaultUser() != null) {
                        dto.setIconUrl(webhook.getDefaultUser().getAvatarUrl());
                        dto.setName(webhook.getDefaultUser().getName());
                    }
                    dto.setChannelId(webhook.getChannel().getId());
                }
            }
        }
        return dto;
    }

    protected void updateWebHook(WebHookOwnedEntity entity, SubscriptionDto dto) {
        WebHook webHook = entity.getWebHook();
        webHook.setEnabled(dto.isEnabled());
        if (webHook.isEnabled() && dto.getChannelId() != null) {
            webHookService.updateWebHook(entity.getGuildConfig().getGuildId(), Long.valueOf(dto.getChannelId()), webHook, dto.getName());
            webHookService.invalidateCache(entity.getGuildConfig().getGuildId());
        }
        webHookRepository.save(webHook);
    }

    protected <V> V getValue(Map<String, ?> data, String key, Class<V> type) {
        if (data == null) {
            return null;
        }
        Object value = data.get(key);
        return value != null && type.isAssignableFrom(value.getClass()) ? (V) value : null;
    }
}
