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
import ru.caramel.juniperbot.web.dao.ModerationDao;
import ru.caramel.juniperbot.web.dto.config.ModerationConfigDto;

@RestController
public class ModerationController extends BaseRestController {

    @Autowired
    private ModerationDao moderationDao;

    @RequestMapping(value = "/moderation/{guildId}", method = RequestMethod.GET)
    @ResponseBody
    public ModerationConfigDto load(@GuildId @PathVariable long guildId) {
        return moderationDao.getConfig(guildId);
    }

    @RequestMapping(value = "/moderation/{guildId}", method = RequestMethod.POST)
    public void save(@GuildId @PathVariable long guildId,
                     @RequestBody @Validated ModerationConfigDto configDto) {
        moderationDao.saveConfig(configDto, guildId);
    }
}
