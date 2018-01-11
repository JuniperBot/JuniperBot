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
package ru.caramel.juniperbot.modules.audio.service;

import net.dv8tion.jda.core.events.ReconnectedEvent;
import net.dv8tion.jda.core.events.ResumedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.caramel.juniperbot.modules.audio.service.PlayerService;
import ru.caramel.juniperbot.core.service.listeners.DiscordEventListener;

@Component
public class ReconnectListener extends DiscordEventListener {

    @Autowired
    private PlayerService playerService;

    @Override
    public void onResume(ResumedEvent event) {
        playerService.reconnectAll();
    }

    @Override
    public void onReconnect(ReconnectedEvent event) {
        playerService.reconnectAll();
    }
}
