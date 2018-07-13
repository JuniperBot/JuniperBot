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
package ru.caramel.juniperbot.web.controller.front.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import ru.caramel.juniperbot.module.ranking.service.RankingService;
import ru.caramel.juniperbot.web.common.navigation.Navigation;
import ru.caramel.juniperbot.web.common.navigation.PageElement;
import ru.caramel.juniperbot.web.controller.front.AbstractController;

@Controller
@Navigation(PageElement.RANKING)
public class RankingController extends AbstractController {

    @Autowired
    private RankingService rankingService;

    @RequestMapping(value = "/ranking/resetAll/{serverId}", method = RequestMethod.POST)
    @ResponseBody
    public String resetAll(
            @PathVariable("serverId") long serverId) {
        validateGuildId(serverId);
        rankingService.resetAll(serverId);
        return "ok";
    }

    @RequestMapping(value = "/ranking/update/{serverId}", method = RequestMethod.POST)
    @ResponseBody
    public String update(
            @PathVariable("serverId") long serverId,
            @RequestParam("userId") long userId,
            @RequestParam("level") int level) {
        validateGuildId(serverId);
        rankingService.setLevel(serverId, userId, level);
        return "ok";
    }
}
