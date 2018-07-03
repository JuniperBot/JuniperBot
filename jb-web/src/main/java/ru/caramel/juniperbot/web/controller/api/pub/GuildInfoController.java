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
package ru.caramel.juniperbot.web.controller.api.pub;

import net.dv8tion.jda.core.entities.Guild;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.caramel.juniperbot.core.model.exception.NotFoundException;
import ru.caramel.juniperbot.core.persistence.entity.GuildConfig;
import ru.caramel.juniperbot.core.service.ConfigService;
import ru.caramel.juniperbot.core.service.DiscordService;
import ru.caramel.juniperbot.web.controller.api.base.BasePublicRestController;
import ru.caramel.juniperbot.web.dto.api.GuildInfoDto;
import ru.caramel.juniperbot.web.dto.api.request.GuildInfoRequest;

@RestController
public class GuildInfoController extends BasePublicRestController {

    @Autowired
    private DiscordService discordService;

    @Autowired
    private ConfigService configService;

    @RequestMapping(value = "/guildInfo", method = RequestMethod.POST)
    @ResponseBody
    public GuildInfoDto getInfo(@RequestBody GuildInfoRequest request) {
        GuildConfig config = configService.getById(request.getId());
        if (config == null) {
            throw new NotFoundException();
        }
        GuildInfoDto.Builder builder = GuildInfoDto.builder()
                .name(config.getName())
                .id(String.valueOf(config.getGuildId()))
                .icon(config.getIconUrl());
        if (discordService.isConnected()) {
            Guild guild = discordService.getGuildById(request.getId());
            if (guild != null && guild.isAvailable()) {
                builder.name(guild.getName())
                        .id(guild.getId())
                        .icon(guild.getIconUrl())
                        .available(true);
            }
        }
        return builder.build();
    }
}
