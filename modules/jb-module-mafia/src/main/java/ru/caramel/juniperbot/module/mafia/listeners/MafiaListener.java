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

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.MessageType;
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import ru.caramel.juniperbot.core.event.DiscordEvent;
import ru.caramel.juniperbot.core.event.listeners.DiscordEventListener;
import ru.caramel.juniperbot.core.message.service.MessageService;
import ru.caramel.juniperbot.module.mafia.model.MafiaInstance;
import ru.caramel.juniperbot.module.mafia.model.MafiaState;
import ru.caramel.juniperbot.module.mafia.service.MafiaService;

import java.util.Objects;

@DiscordEvent
public class MafiaListener extends DiscordEventListener {

    @Autowired
    private MafiaService mafiaService;

    @Autowired
    private MessageService messageService;

    @Override
    public void onGuildLeave(GuildLeaveEvent event) {
        mafiaService.stop(event.getGuild());
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot() || event.getMessage().getType() != MessageType.DEFAULT) {
            return;
        }
        switch (event.getChannelType()) {
            case PRIVATE:
            case TEXT:
                MafiaInstance instance = mafiaService.getRelatedInstance(event.getChannel().getIdLong());
                if (instance != null && !instance.isInState(MafiaState.FINISH)) {
                    boolean isPlayer = instance.isPlayer(event.getAuthor());
                    if (isPlayer) {
                        instance.tick();
                    }

                    if (event.getChannelType().isGuild()) {
                        Member selfMember = event.getGuild().getSelfMember();
                        if (!event.getMember().equals(selfMember)
                                && !instance.isInState(MafiaState.CHOOSING)
                                && !Objects.equals(event.getChannel().getIdLong(), instance.getGoonChannelId())
                                && selfMember.hasPermission(event.getTextChannel(), Permission.MESSAGE_MANAGE)) {
                            if (instance.isInState(MafiaState.DAY)) {
                                if (!isPlayer) {
                                    messageService.delete(event.getMessage());
                                }
                            } else {
                                messageService.delete(event.getMessage());
                            }
                        }
                    }
                }
                break;
        }
    }
}
