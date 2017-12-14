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
package ru.caramel.juniperbot.web.ranking.controller;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import ru.caramel.juniperbot.model.exception.NotFoundException;
import ru.caramel.juniperbot.persistence.entity.GuildConfig;
import ru.caramel.juniperbot.ranking.model.RankingInfo;
import ru.caramel.juniperbot.ranking.service.RankingService;
import ru.caramel.juniperbot.security.utils.SecurityUtils;
import ru.caramel.juniperbot.web.common.AbstractController;
import ru.caramel.juniperbot.web.common.navigation.Navigation;
import ru.caramel.juniperbot.web.common.navigation.PageElement;

import java.util.Collections;
import java.util.List;

@Controller
public class RankingController extends AbstractController {

    @Autowired
    private RankingService rankingService;

    @RequestMapping("/ranking/{serverId}")
    @Navigation(PageElement.RANKING)
    public ModelAndView view(@PathVariable long serverId,
                             @RequestParam(value = "forceUser", required = false, defaultValue = "false") boolean forceUser) {
        ModelAndView mv;
        if (!forceUser && isAuthorized(serverId)) {
            mv = createModel("ranking.admin", serverId);
        } else {
            if (!rankingService.checkExists(serverId)) {
                throw new NotFoundException();
            }
            mv = createModel("ranking.user", serverId, false);
        }
        return mv;
    }

    @RequestMapping("/ranking/list/{serverId}")
    public ModelAndView list(@PathVariable long serverId) {
        ModelAndView mv = new ModelAndView("ranking.list");
        List<RankingInfo> members = rankingService.getRankingInfos(serverId);
        return mv
                .addObject("editable", isAuthorized(serverId))
                .addObject("members", members);
    }

    @RequestMapping(value = "/ranking/reset/{serverId}", method = RequestMethod.POST)
    @ResponseBody
    public String reset(
            @PathVariable("serverId") long serverId,
            @RequestParam("userId") long userId) {
        validateGuildId(serverId);
        rankingService.setLevel(serverId, userId, 0);
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

    private boolean isAuthorized(long serverId) {
        return SecurityUtils.isAuthenticated() && isGuildAuthorized(serverId);
    }
}
