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
package ru.juniperbot.common.service;

import ru.juniperbot.common.model.command.CommandInfo;
import ru.juniperbot.common.model.discord.GuildDto;
import ru.juniperbot.common.model.discord.WebhookDto;
import ru.juniperbot.common.model.request.PatreonRequest;
import ru.juniperbot.common.model.request.RankingUpdateRequest;
import ru.juniperbot.common.model.request.WebhookRequest;
import ru.juniperbot.common.model.status.StatusDto;

import java.util.List;

public interface GatewayService {

    GuildDto getGuildInfo(long guildId);

    void updateRanking(RankingUpdateRequest request);

    List<CommandInfo> getCommandList();

    StatusDto getWorkerStatus();

    WebhookDto getWebhook(WebhookRequest request);

    void updateWebhook(WebhookDto webhook);

    boolean deleteWebhook(WebhookRequest request);

    boolean sendPatreonUpdate(PatreonRequest request);
}
