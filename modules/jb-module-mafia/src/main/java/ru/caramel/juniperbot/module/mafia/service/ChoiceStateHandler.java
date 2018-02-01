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

import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.PermissionOverride;
import org.apache.commons.lang3.ArrayUtils;
import ru.caramel.juniperbot.core.listeners.ReactionsListener;
import ru.caramel.juniperbot.module.mafia.model.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public abstract class ChoiceStateHandler extends AbstractStateHandler {

    @SuppressWarnings("unchecked")
    protected MafiaPlayer getChoiceResult(MafiaInstance instance) {
        MafiaPlayer target = null;
        Map<MafiaPlayer, Set<MafiaPlayer>> choices = (Map<MafiaPlayer, Set<MafiaPlayer>>) instance.removeAttribute(getChoiceKey());
        if (choices != null) {
            int size = 0;
            for (Map.Entry<MafiaPlayer, Set<MafiaPlayer>> entry : choices.entrySet()) {
                if (entry.getValue().size() > size) {
                    size = entry.getValue().size();
                    target = entry.getKey();
                }
            }
        }
        return target;
    }

    protected void sendChoice(MafiaInstance instance, Message message) {
        List<MafiaPlayer> players = instance.getAlive();
        Map<MafiaPlayer, Set<MafiaPlayer>> choices = new ConcurrentHashMap<>(players.size());
        instance.putAttribute(getChoiceKey(), choices);
        try {
            for (int i = 0; i < players.size(); i++) {
                message.addReaction(ReactionsListener.CHOICES[i]).submit();
            }
        } catch (Exception ex) {
            // ignore
        }

        instance.getListenedMessages().add(message.getId());
        reactionsListener.onReaction(message.getId(), (event, add) -> {
            if (!event.getUser().equals(event.getJDA().getSelfUser()) && !event.getUser().isBot() && instance.isInState(getState())) {
                String emote = event.getReaction().getReactionEmote().getName();
                int index = ArrayUtils.indexOf(ReactionsListener.CHOICES, emote);
                if (index >= 0 && index < players.size()) {
                    MafiaPlayer target = players.get(index);
                    MafiaPlayer chooser = instance.getPlayerByUser(event.getUser());
                    if (target != null && chooser != null) {
                        instance.tick();
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

    protected void outPlayer(MafiaInstance instance, MafiaPlayer player) {
        player.out();
        if (player.getRole() == MafiaRole.GOON && instance.getGoonChannel() != null) {
            PermissionOverride override = instance.getGoonChannel().getPermissionOverride(player.getMember());
            override.delete().submit();
        }
    }

    protected abstract String getChoiceKey();

    protected abstract MafiaState getState();
}
