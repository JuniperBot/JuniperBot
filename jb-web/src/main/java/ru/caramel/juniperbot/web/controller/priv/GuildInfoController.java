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
import org.springframework.web.bind.annotation.*;
import ru.caramel.juniperbot.core.model.exception.NotFoundException;
import ru.caramel.juniperbot.web.controller.base.BaseRestController;
import ru.caramel.juniperbot.web.dao.GuildDao;
import ru.caramel.juniperbot.web.dto.discord.GuildDto;
import ru.caramel.juniperbot.web.dto.request.GuildInfoRequest;

@RestController
public class GuildInfoController extends BaseRestController {

    @Autowired
    private GuildDao guildDao;

    @RequestMapping(value = "/guild", method = RequestMethod.POST)
    @ResponseBody
    public GuildDto getGuild(@RequestBody GuildInfoRequest request) {
        GuildDto dto = guildDao.getGuild(request);
        if (dto == null) {
            throw new NotFoundException();
        }
        return dto;
    }
}
