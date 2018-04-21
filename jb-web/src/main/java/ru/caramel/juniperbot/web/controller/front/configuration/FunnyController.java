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
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import ru.caramel.juniperbot.web.common.navigation.Navigation;
import ru.caramel.juniperbot.web.common.navigation.PageElement;
import ru.caramel.juniperbot.web.controller.front.AbstractController;
import ru.caramel.juniperbot.web.dao.FunnyDao;
import ru.caramel.juniperbot.web.dto.FunnyConfigDto;

@Controller
@Navigation(PageElement.FUNNY)
public class FunnyController extends AbstractController {

    @Autowired
    private FunnyDao funnyDao;

    @RequestMapping("/funny/{serverId}")
    public ModelAndView view(@PathVariable long serverId) {
        validateGuildId(serverId);
        return createModel("funny", serverId)
                .addObject("funny", funnyDao.getConfig(serverId));
    }

    @RequestMapping(value = "/funny/{serverId}", method = RequestMethod.POST)
    public ModelAndView save(
            @PathVariable long serverId,
            @Validated @ModelAttribute("funny") FunnyConfigDto funnyDto,
            BindingResult result) {
        validateGuildId(serverId);
        if (result.hasErrors()) {
            return createModel("funny", serverId);
        }
        funnyDao.save(serverId, funnyDto);
        flash.success("flash.funny.save.success.message");
        return view(serverId);
    }
}
