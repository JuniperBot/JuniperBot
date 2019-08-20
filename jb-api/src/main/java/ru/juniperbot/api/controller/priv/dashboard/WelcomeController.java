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
package ru.juniperbot.api.controller.priv.dashboard;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.juniperbot.api.common.aspect.GuildId;
import ru.juniperbot.api.controller.base.BaseRestController;
import ru.juniperbot.api.dao.WelcomeDao;
import ru.juniperbot.api.dto.config.WelcomeDto;

@RestController
public class WelcomeController extends BaseRestController {

    @Autowired
    private WelcomeDao welcomeDao;

    @RequestMapping(value = "/welcome/{guildId}", method = RequestMethod.GET)
    @ResponseBody
    public WelcomeDto load(@GuildId @PathVariable long guildId) {
        return welcomeDao.get(guildId);
    }

    @RequestMapping(value = "/welcome/{guildId}", method = RequestMethod.POST)
    public void save(@GuildId @PathVariable long guildId,
                     @RequestBody @Validated WelcomeDto dto) {
        welcomeDao.save(dto, guildId);
    }
}
