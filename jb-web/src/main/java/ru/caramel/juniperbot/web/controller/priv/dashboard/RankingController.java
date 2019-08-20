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
package ru.caramel.juniperbot.web.controller.priv.dashboard;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.caramel.juniperbot.web.common.aspect.GuildId;
import ru.caramel.juniperbot.web.controller.base.BaseRestController;
import ru.caramel.juniperbot.web.dao.RankingDao;
import ru.caramel.juniperbot.web.dto.config.RankingDto;
import ru.caramel.juniperbot.web.dto.request.RankingResetRequest;
import ru.juniperbot.common.model.request.RankingUpdateRequest;
import ru.juniperbot.common.service.RankingConfigService;

@RestController
public class RankingController extends BaseRestController {

    @Autowired
    private RankingDao rankingDao;

    @Autowired
    private RankingConfigService rankingConfigService;

    @RequestMapping(value = "/ranking/{guildId}", method = RequestMethod.GET)
    @ResponseBody
    public RankingDto load(@GuildId @PathVariable long guildId) {
        return rankingDao.get(guildId);
    }

    @RequestMapping(value = "/ranking/{guildId}", method = RequestMethod.POST)
    public void save(@GuildId @PathVariable long guildId,
                     @RequestBody @Validated RankingDto dto) {
        rankingDao.save(dto, guildId);
    }

    @RequestMapping(value = "/ranking/reset/{guildId}", method = RequestMethod.POST)
    public void resetAll(
            @GuildId @PathVariable("guildId") long guildId,
            @RequestBody @Validated RankingResetRequest request) {
        rankingConfigService.resetAll(guildId, request.isLevels(), request.isCookies());
    }

    @RequestMapping(value = "/ranking/update/{guildId}", method = RequestMethod.POST)
    public void update(
            @GuildId @PathVariable("guildId") long guildId,
            @RequestBody @Validated RankingUpdateRequest request) {
        request.setGuildId(guildId);
        rankingConfigService.update(request);
        gatewayService.updateRanking(request);
    }
}
