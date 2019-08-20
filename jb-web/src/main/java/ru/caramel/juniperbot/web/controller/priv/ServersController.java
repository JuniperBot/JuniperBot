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
package ru.caramel.juniperbot.web.controller.priv;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import ru.caramel.juniperbot.web.controller.base.BaseRestController;
import ru.caramel.juniperbot.web.dto.ServersDto;
import ru.caramel.juniperbot.web.security.auth.DiscordTokenServices;
import ru.caramel.juniperbot.web.security.model.DiscordGuildDetails;
import ru.juniperbot.common.model.discord.GuildDto;

import java.util.List;

@RestController
public class ServersController extends BaseRestController {

    @Autowired
    private DiscordTokenServices discordTokenServices;

    @RequestMapping(value = "/servers", method = RequestMethod.GET)
    @ResponseBody
    public ServersDto getServers() {
        List<DiscordGuildDetails> detailsList = discordTokenServices.getCurrentGuilds(true);
        ServersDto result = new ServersDto();
        result.setGuilds(apiMapperService.getGuildDtos(detailsList));
        result.getGuilds().forEach(e -> {
            GuildDto guild = gatewayService.getGuildInfo(Long.valueOf(e.getId()));
            if (guild != null) {
                e.setAdded(true);
                result.setConnected(true);
                e.setMembers(guild.getOnlineCount());
            }
        });
        return result;
    }
}
