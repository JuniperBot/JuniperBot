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
package ru.caramel.juniperbot.module.mafia.listeners;

import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import org.springframework.beans.factory.annotation.Autowired;
import ru.juniperbot.worker.common.event.DiscordEvent;
import ru.juniperbot.worker.common.event.listeners.DiscordEventListener;
import ru.caramel.juniperbot.module.mafia.service.MafiaService;

@DiscordEvent
public class MafiaListener extends DiscordEventListener {

    @Autowired
    private MafiaService mafiaService;

    @Override
    public void onGuildLeave(GuildLeaveEvent event) {
        mafiaService.stop(event.getGuild());
    }
}
