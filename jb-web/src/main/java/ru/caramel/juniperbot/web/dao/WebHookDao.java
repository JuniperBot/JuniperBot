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
package ru.caramel.juniperbot.web.dao;

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Webhook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.caramel.juniperbot.core.persistence.entity.WebHook;
import ru.caramel.juniperbot.core.service.WebHookService;
import ru.caramel.juniperbot.web.dto.WebHookDto;

@Service
public class WebHookDao extends AbstractDao {

    @Autowired
    private WebHookService webHookService;

    public WebHookDto getDtoForView(long guildId, WebHook webHook) {
        WebHookDto hookDto = mapper.getWebHookDto(webHook);
        if (discordService.isConnected(guildId)) {
            Guild guild = discordService.getShardManager().getGuildById(guildId);
            if (guild != null && guild.getSelfMember().hasPermission(Permission.MANAGE_WEBHOOKS)) {
                hookDto.setAvailable(true);
                Webhook webhook = webHookService.getWebHook(guild, webHook);
                if (webhook != null) {
                    hookDto.setChannelId(webhook.getChannel().getIdLong());
                } else {
                    hookDto.setEnabled(false);
                }
            }
        }
        return hookDto;
    }
}
