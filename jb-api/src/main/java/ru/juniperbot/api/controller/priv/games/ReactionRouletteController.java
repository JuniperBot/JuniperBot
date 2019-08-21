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
package ru.juniperbot.api.controller.priv.games;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.juniperbot.api.common.aspect.GuildId;
import ru.juniperbot.api.controller.base.BaseRestController;
import ru.juniperbot.api.dao.ReactionRouletteDao;
import ru.juniperbot.api.dto.games.ReactionRouletteConfigDto;
import ru.juniperbot.common.model.discord.GuildDto;

import java.util.stream.Collectors;

@RestController
public class ReactionRouletteController extends BaseRestController {

    @Autowired
    private ReactionRouletteDao rouletteDao;

    @RequestMapping(value = "/games/roulette/{guildId}", method = RequestMethod.GET)
    @ResponseBody
    public ReactionRouletteConfigDto load(@GuildId @PathVariable long guildId) {
        ReactionRouletteConfigDto configDto = new ReactionRouletteConfigDto();
        configDto.setConfig(rouletteDao.get(guildId));
        GuildDto guildDto = gatewayService.getGuildInfo(guildId);
        configDto.setEmotes(guildDto.getEmotes().stream().filter(e -> !e.isManaged()).collect(Collectors.toList()));
        return configDto;
    }

    @RequestMapping(value = "/games/roulette/{guildId}", method = RequestMethod.POST)
    public void save(@GuildId @PathVariable long guildId,
                     @RequestBody @Validated ReactionRouletteConfigDto configDto) {
        rouletteDao.save(configDto.getConfig(), guildId);
    }
}
