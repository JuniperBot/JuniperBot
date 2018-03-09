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
package ru.caramel.juniperbot.web.controller.front;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import ru.caramel.juniperbot.core.model.exception.NotFoundException;
import ru.caramel.juniperbot.core.persistence.entity.GuildConfig;
import ru.caramel.juniperbot.module.audio.persistence.entity.Playlist;
import ru.caramel.juniperbot.module.audio.service.PlaylistService;

@Controller
public class PlaylistController extends AbstractController {

    @Autowired
    private PlaylistService playlistService;

    @RequestMapping("/playlist/{uuid}")
    @Transactional(readOnly = true)
    public ModelAndView status(@PathVariable String uuid) {
        Playlist playlist = playlistService.getPlaylist(uuid);
        if (playlist == null) {
            throw new NotFoundException();
        }
        GuildConfig config = playlist.getGuildConfig();
        return createModel("playlist", config.getGuildId())
                .addObject("prefix", config.getPrefix())
                .addObject("playlist", playlist);
    }
}
