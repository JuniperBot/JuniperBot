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
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.User;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.caramel.juniperbot.core.listeners.ReactionsListener;
import ru.caramel.juniperbot.module.mafia.model.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GoonHandler extends AbstractStateHandler {

    @Autowired
    private BrokerHandler brokerHandler;

    private final static String ATTR_CHOICES = "GoonHandler.Choices";

    @Override
    public boolean onStart(User user, MafiaInstance instance) {
        instance.setState(MafiaState.NIGHT_GOON);
        instance.getChannel().sendMessage(getBaseEmbed("mafia.night.start").build()).complete();
        sendChoiceMessage(instance);
        return scheduleEnd(instance, dayDelay);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean onEnd(User user, MafiaInstance instance) {
        if (!instance.isPlayer(user, MafiaRole.GOON)) {
            return false;
        }

        Map<MafiaPlayer, Set<MafiaPlayer>> choices = (Map<MafiaPlayer, Set<MafiaPlayer>>) instance.removeAttribute(ATTR_CHOICES);
        if (choices != null) {
            int size = 0;
            MafiaPlayer target = null;
            for (Map.Entry<MafiaPlayer, Set<MafiaPlayer>> entry : choices.entrySet()) {
                if (entry.getValue().size() > size) {
                    size = entry.getValue().size();
                    target = entry.getKey();
                }
            }
            if (target != null) {
                instance.getDailyActions().put(MafiaActionType.KILL, target);
            }
        }
        return brokerHandler.onStart(user, instance);
    }

    public void sendChoiceMessage(MafiaInstance instance) {
        List<MafiaPlayer> players = new ArrayList<>(instance.getAlive());
        MessageBuilder builder = new MessageBuilder();
        EmbedBuilder embed = getBaseEmbed("mafia.goon.choice");
        embed.addField(messageService.getMessage("mafia.start.playerList.title"),
                getPlayerList(players), false);
        embed.setFooter(messageService.getMessage("mafia.goon.choice.footer",
                getEndTimeText(instance, dayDelay), instance.getPrefix()), null);
        builder.setEmbed(embed.build());
        builder.setContent(instance.getGoonsMentions());

        Message message = instance.getGoonChannel().sendMessage(builder.build()).complete();

        Map<MafiaPlayer, Set<MafiaPlayer>> choices = new ConcurrentHashMap<>(players.size());
        instance.putAttribute(ATTR_CHOICES, choices);
        try {
            for (int i = 0; i < players.size(); i++) {
                message.addReaction(ReactionsListener.CHOICES[i]).submit();
            }
        } catch (Exception ex) {
            // ignore
        }
        instance.getListenedMessages().add(message.getId());
        reactionsListener.onReaction(message.getId(), (event, add) -> {
            if (!event.getUser().equals(event.getJDA().getSelfUser()) && instance.isInState(MafiaState.NIGHT_GOON)) {
                String emote = event.getReaction().getReactionEmote().getName();
                int index = ArrayUtils.indexOf(ReactionsListener.CHOICES, emote);
                if (index >= 0 && index < players.size()) {
                    MafiaPlayer target = players.get(index);
                    MafiaPlayer chooser = instance.getPlayerByUser(event.getUser());
                    if (target != null && chooser != null && chooser.getRole() == MafiaRole.GOON) {
                        Set<MafiaPlayer> choosers = choices.computeIfAbsent(target, c -> Collections.synchronizedSet(new HashSet<>()));
                        if (add) {
                            choosers.add(chooser);
                        } else {
                            choosers.remove(chooser);
                        }
                    }
                }
            }
            return false;
        });
    }

}
