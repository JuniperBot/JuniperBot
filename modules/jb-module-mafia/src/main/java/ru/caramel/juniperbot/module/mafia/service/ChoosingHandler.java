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
package ru.caramel.juniperbot.module.mafia.service;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.caramel.juniperbot.module.mafia.model.MafiaInstance;
import ru.caramel.juniperbot.module.mafia.model.MafiaPlayer;
import ru.caramel.juniperbot.module.mafia.model.MafiaRole;
import ru.caramel.juniperbot.module.mafia.model.MafiaState;
import ru.caramel.juniperbot.module.mafia.service.base.AbstractStateHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@Component
public class ChoosingHandler extends AbstractStateHandler {

    private static final String CHOOSE = "✅";

    @Autowired
    private MeetingHandler meetingHandler;

    @Override
    public boolean onStart(User user, MafiaInstance instance) {
        instance.tick();
        String delayText = getEndTimeText(instance, choosingDelay);
        EmbedBuilder builder = getBaseEmbed("mafia.start.message");
        builder.setFooter(messageService.getMessage("mafia.start.message.footer", delayText, instance.getPrefix()), null);
        Message message = instance.getChannel().sendMessage(builder.build()).complete();
        message.addReaction(CHOOSE).submit();
        instance.getListenedMessages().add(message.getId());
        reactionsListener.onReaction(message.getId(), (event, add) -> {
            if (!instance.isInState(MafiaState.CHOOSING)) {
                return true;
            }
            String emote = event.getReaction().getReactionEmote().getName();
            if (!event.getUser().equals(event.getJDA().getSelfUser()) && !event.getUser().isBot() && CHOOSE.equals(emote)) {
                instance.tick();
                if (add && instance.getPlayers().size() < 10) {
                    instance.getPlayers().add(new MafiaPlayer(event.getMember()));
                }
                if (!add) {
                    instance.getPlayers().removeIf(e -> event.getMember().equals(e.getMember()));
                }
            }
            return false;
        });
        return scheduleEnd(instance, choosingDelay);
    }

    @Override
    public boolean onEnd(User user, MafiaInstance instance) {
        instance.setState(MafiaState.MEETING);
        int playerCount = instance.getPlayers().size();
        int minPlayers = debug ? 2 : 3;
        if (playerCount < minPlayers) {
            instance.setEndReason(messageService.getMessage("mafia.start.minPlayers.title", minPlayers));
            return true;
        }
        Iterator<MafiaRole> roles = getRoles(playerCount).iterator();
        instance.getPlayers().forEach(e -> e.setRole(roles.next()));
        return meetingHandler.onStart(user, instance);
    }

    private List<MafiaRole> getRoles(int playerCount) {
        List<MafiaRole> roleList = new ArrayList<>(playerCount);
        roleList.add(MafiaRole.GOON);
        if (playerCount > 4) {
            roleList.add(MafiaRole.DOCTOR);
            roleList.add(MafiaRole.BROKER);
        }
        if (playerCount > 6) {
            roleList.add(MafiaRole.COP);
        }
        if (playerCount > 7) {
            roleList.add(MafiaRole.GOON);
        }
        for (int i = 0; i < playerCount - roleList.size(); i++) {
            roleList.add(MafiaRole.TOWNIE);
        }
        Collections.shuffle(roleList);
        return roleList;
    }
}
