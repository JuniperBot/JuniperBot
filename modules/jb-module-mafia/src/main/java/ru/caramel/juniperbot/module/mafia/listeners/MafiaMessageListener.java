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

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.caramel.juniperbot.core.listeners.DiscordEventListener;
import ru.caramel.juniperbot.module.mafia.model.MafiaInstance;
import ru.caramel.juniperbot.module.mafia.model.MafiaState;
import ru.caramel.juniperbot.module.mafia.service.MafiaService;

@Component
public class MafiaMessageListener extends DiscordEventListener {

    @Autowired
    private MafiaService mafiaService;

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        switch (event.getChannelType()) {
            case PRIVATE:
            case TEXT:
                MafiaInstance instance = mafiaService.getRelatedInstance(event.getChannel().getIdLong());
                if (instance != null && !instance.isInState(MafiaState.FINISH) && instance.isPlayer(event.getAuthor())) {
                    instance.tick();
                }
                break;
        }
    }
}
