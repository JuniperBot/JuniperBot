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
package ru.juniperbot.worker.rabbit;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Webhook;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.juniperbot.common.configuration.RabbitConfiguration;
import ru.juniperbot.common.model.discord.WebhookDto;
import ru.juniperbot.common.model.request.WebhookRequest;
import ru.juniperbot.common.service.DiscordMapperService;
import ru.juniperbot.common.worker.modules.notification.service.WebHookService;

@EnableRabbit
@Component
@Slf4j
public class WebhookQueueListener extends BaseQueueListener {

    @Autowired
    private WebHookService webHookService;

    @Autowired
    private DiscordMapperService discordMapperService;

    @RabbitListener(queues = RabbitConfiguration.QUEUE_WEBHOOK_GET_REQUEST)
    public WebhookDto getWebhook(WebhookRequest request) {
        Guild guild = getGuildById(request.getGuildId());
        if (guild == null || !guild.getSelfMember().hasPermission(Permission.MANAGE_WEBHOOKS)) {
            return WebhookDto.EMPTY;
        }
        Webhook webhook = webHookService.getWebHook(request.getId(), guild);
        if (webhook == null) {
            return WebhookDto.EMPTY;
        }
        WebhookDto dto = discordMapperService.getWebhookDto(webhook);
        dto.setId(request.getId());
        return dto;
    }

    @RabbitListener(queues = RabbitConfiguration.QUEUE_WEBHOOK_UPDATE_REQUEST)
    public void updateWebhook(WebhookDto request) {
        webHookService.updateWebHook(request.getId(), request.getGuildId(), request.getChannelId(), request.getName(),
                request.getIconUrl());
    }

    @RabbitListener(queues = RabbitConfiguration.QUEUE_WEBHOOK_DELETE_REQUEST)
    public boolean deleteWebhook(WebhookRequest request) {
        Guild guild = getGuildById(request.getGuildId());
        if (guild == null || !guild.getSelfMember().hasPermission(Permission.MANAGE_WEBHOOKS)) {
            return false;
        }
        return webHookService.delete(request.getId(), request.getGuildId());
    }
}
