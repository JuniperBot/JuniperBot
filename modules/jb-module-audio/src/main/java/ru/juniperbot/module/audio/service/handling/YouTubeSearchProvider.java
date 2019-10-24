/*
 * This file is part of JuniperBot.
 *
 * JuniperBot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * JuniperBot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with JuniperBot. If not, see <http://www.gnu.org/licenses/>.
 */
package ru.juniperbot.module.audio.service.handling;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.juniperbot.common.service.YouTubeService;
import ru.juniperbot.module.audio.service.AudioSearchProvider;

@Slf4j
@Component
public class YouTubeSearchProvider implements AudioSearchProvider {

    @Autowired
    private YouTubeService youTubeService;

    @Override
    public String searchTrack(String value) {
        String result = youTubeService.searchForUrl(value);
        return result != null ? result : "ytsearch:" + value;
    }

    @Override
    public String getProviderName() {
        return "youTube";
    }
}
