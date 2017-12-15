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

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Role;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import ru.caramel.juniperbot.model.dto.RankingConfigDto;
import ru.caramel.juniperbot.model.exception.NotFoundException;
import ru.caramel.juniperbot.persistence.entity.GuildConfig;
import ru.caramel.juniperbot.persistence.entity.RankingConfig;
import ru.caramel.juniperbot.ranking.model.RankingInfo;
import ru.caramel.juniperbot.ranking.model.Reward;
import ru.caramel.juniperbot.ranking.model.RewardDetails;
import ru.caramel.juniperbot.ranking.service.RankingService;
import ru.caramel.juniperbot.security.utils.SecurityUtils;
import ru.caramel.juniperbot.service.MapperService;
import ru.caramel.juniperbot.web.common.AbstractController;
import ru.caramel.juniperbot.web.common.navigation.Navigation;
import ru.caramel.juniperbot.web.common.navigation.PageElement;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Controller
@Navigation(PageElement.RANKING)
public class RankingController extends AbstractController {

    @Autowired
    private RankingService rankingService;

    @Autowired
    private MapperService mapperService;

    @RequestMapping("/ranking/{serverId}")
    public ModelAndView view(@PathVariable long serverId,
                             @RequestParam(value = "forceUser", required = false, defaultValue = "false") boolean forceUser) {
        ModelAndView mv;
        GuildConfig config = configService.getOrCreate(serverId, GuildConfig.RANKING_GRAPH);
        if (!forceUser && isAuthorized(serverId)) {
            RankingConfigDto configDto = mapperService.getRankingDto(config.getRankingConfig());
            mv = createModel("ranking.admin", serverId)
                    .addObject("config", configDto)
                    .addObject("rolesManageable", hasPermission(serverId, Permission.MANAGE_ROLES))
                    .addObject("roles", getRoles(serverId));
        } else {
            if (!rankingService.checkExists(serverId)) {
                throw new NotFoundException();
            }
            mv = createModel("ranking.user", serverId, false)
                    .addObject("rewards", getRewards(serverId, config.getRankingConfig()));
        }
        return mv.addObject("prefix", config.getPrefix());
    }

    @RequestMapping(value = "/ranking/{serverId}", method = RequestMethod.POST)
    public ModelAndView save(
            @PathVariable long serverId,
            @Validated @ModelAttribute("config") RankingConfigDto config,
            BindingResult result) {
        validateGuildId(serverId);
        if (result.hasErrors()) {
            GuildConfig guildConfig = configService.getOrCreate(serverId, GuildConfig.RANKING_GRAPH);
            return createModel("ranking.admin", serverId)
                    .addObject("prefix", guildConfig.getPrefix())
                    .addObject("rolesManageable", hasPermission(serverId, Permission.MANAGE_ROLES))
                    .addObject("roles", getRoles(serverId));
        }
        rankingService.saveConfig(config, serverId);
        flash.success("flash.rating.save.success.message");
        return view(serverId, false);
    }

    @RequestMapping("/ranking/list/{serverId}")
    public ModelAndView list(@PathVariable long serverId) {
        ModelAndView mv = new ModelAndView("ranking.list");
        List<RankingInfo> members = rankingService.getRankingInfos(serverId);
        return mv
                .addObject("editable", isAuthorized(serverId))
                .addObject("members", members);
    }

    @RequestMapping(value = "/ranking/sync/{serverId}", method = RequestMethod.POST)
    @ResponseBody
    public String sync(
            @PathVariable("serverId") long serverId) {
        validateGuildId(serverId);
        if (discordClient.isConnected()) {
            Guild guild = discordClient.getJda().getGuildById(serverId);
            if (guild != null) {
                rankingService.sync(guild);
                return "ok";
            }
        }
        return "fail";
    }

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

    private boolean isAuthorized(long serverId) {
        return SecurityUtils.isAuthenticated() && isGuildAuthorized(serverId);
    }

    private List<RewardDetails> getRewards(long serverId, RankingConfig config) {
        if (discordClient.isConnected() && CollectionUtils.isNotEmpty(config.getRewards())) {
            Guild guild = discordClient.getJda().getGuildById(serverId);
            if (guild != null) {
                List<RewardDetails> details = new ArrayList<>();
                for (Reward reward : config.getRewards()) {
                    Role role = guild.getRoleById(reward.getRoleId());
                    if (role != null) {
                        details.add(new RewardDetails(role, reward));
                    }
                }
                details.sort(Comparator.comparing(RewardDetails::getLevel));
                return details;
            }

        }
        return null;
    }
}
