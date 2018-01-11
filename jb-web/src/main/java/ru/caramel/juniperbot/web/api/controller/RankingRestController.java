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
package ru.caramel.juniperbot.web.api.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.caramel.juniperbot.core.modules.ranking.model.RankingInfo;
import ru.caramel.juniperbot.core.modules.ranking.service.RankingService;

import java.util.Collections;
import java.util.List;

@RestController
public class RankingRestController extends BaseRestController {

    private static final Logger LOG = LoggerFactory.getLogger(RankingRestController.class);

    @Autowired
    private RankingService rankingService;

    @RequestMapping(value = "/ranking/list/{serverId}", method = RequestMethod.GET)
    @ResponseBody
    public List<RankingInfo> list(
            @PathVariable("serverId") long serverId) {
        return rankingService.isEnabled(serverId) ? rankingService.getRankingInfos(serverId) : Collections.emptyList();
    }
}
