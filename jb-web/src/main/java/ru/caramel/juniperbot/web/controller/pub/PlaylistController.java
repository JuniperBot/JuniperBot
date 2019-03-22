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
package ru.caramel.juniperbot.web.controller.pub;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.caramel.juniperbot.core.common.model.exception.NotFoundException;
import ru.caramel.juniperbot.web.controller.base.BasePublicRestController;
import ru.caramel.juniperbot.web.dao.PlaylistDao;
import ru.caramel.juniperbot.web.dto.playlist.PlaylistDto;

@RestController
public class PlaylistController extends BasePublicRestController {

    @Autowired
    private PlaylistDao playlistDao;

    @RequestMapping("/playlist/{uuid}")
    @ResponseBody
    public PlaylistDto get(@PathVariable String uuid,
                           @RequestParam(value = "withGuild", defaultValue = "true") boolean withGuild) {
        PlaylistDto playlist = playlistDao.get(uuid, withGuild);
        if (playlist == null) {
            throw new NotFoundException();
        }
        return playlist;
    }
}
