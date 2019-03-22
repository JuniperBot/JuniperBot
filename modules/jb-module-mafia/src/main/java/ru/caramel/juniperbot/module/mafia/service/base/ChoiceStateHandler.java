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
package ru.caramel.juniperbot.module.mafia.service.base;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.PermissionOverride;
import net.dv8tion.jda.core.entities.TextChannel;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import ru.caramel.juniperbot.core.event.listeners.ReactionsListener;
import ru.caramel.juniperbot.module.mafia.model.MafiaInstance;
import ru.caramel.juniperbot.module.mafia.model.MafiaPlayer;
import ru.caramel.juniperbot.module.mafia.model.MafiaRole;
import ru.caramel.juniperbot.module.mafia.model.MafiaState;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public abstract class ChoiceStateHandler extends AbstractStateHandler {

    protected static final String ATTR_MESSAGE_ID = "ChoiceState.messageId.";

    @SuppressWarnings("unchecked")
    protected MafiaPlayer getChoiceResult(MafiaInstance instance) {
        boolean duplicate = false;
        MafiaPlayer target = null;
        Map<MafiaPlayer, Set<MafiaPlayer>> choices = (Map<MafiaPlayer, Set<MafiaPlayer>>) instance.removeAttribute(getChoiceKey());
        if (choices != null) {
            int size = 0;
            for (Map.Entry<MafiaPlayer, Set<MafiaPlayer>> entry : choices.entrySet()) {
                if (entry.getValue().size() > size) {
                    size = entry.getValue().size();
                    target = entry.getKey();
                    duplicate = false;
                } else if (entry.getValue().size() == size) {
                    duplicate = true;
                }
            }
        }
        return duplicate ? null : target;
    }

    protected void sendChoice(MafiaInstance instance, Message message, List<MafiaPlayer> choosers) {
        List<MafiaPlayer> players = instance.getAlive();
        Map<MafiaPlayer, Set<MafiaPlayer>> choices = new ConcurrentHashMap<>(players.size());
        instance.putAttribute(getChoiceKey(), choices);
        try {
            for (int i = 0; i < players.size(); i++) {
                message.addReaction(ReactionsListener.CHOICES[i]).queue();
            }
            if (players.size() < 10) {
                message.addReaction(CHOOSE).queue();
            }
        } catch (Exception ex) {
            // ignore
        }

        Set<MafiaPlayer> ready = new HashSet<>(choosers.size());
        pinMessage(instance, message);
        instance.getListenedMessages().add(message.getId());
        reactionsListener.onReaction(message.getId(), (event, add) -> {
            if (!event.getUser().equals(event.getJDA().getSelfUser()) && instance.isInState(getState())) {
                if (add && (event.getUser().isBot() || !instance.isPlayer(event.getUser()))) {
                    event.getReaction().removeReaction(event.getUser()).queue();
                    return false;
                }
                if (instance.isPlayer(event.getUser())) {
                    MafiaPlayer chooser = instance.getPlayerByUser(event.getUser());
                    String emote = event.getReaction().getReactionEmote().getName();
                    if (CHOOSE.equals(emote) && chooser != null) {
                        if (add) {
                            ready.add(chooser);
                        } else {
                            ready.remove(chooser);
                        }
                        if (CollectionUtils.isEqualCollection(ready, choosers)) {
                            contextService.withContextAsync(instance.getGuild(), () -> {
                                if (instance.done(event.getUser())) {
                                    mafiaService.stop(instance);
                                }
                            });
                            return true;
                        }
                        return false;
                    }
                    int index = ArrayUtils.indexOf(ReactionsListener.CHOICES, emote);
                    if (index >= 0 && index < players.size()) {
                        MafiaPlayer target = players.get(index);
                        if (target != null && chooser != null) {
                            instance.tick();
                            Set<MafiaPlayer> choosed = choices.computeIfAbsent(target, c -> Collections.synchronizedSet(new HashSet<>()));
                            if (add) {
                                choosed.add(chooser);
                            } else {
                                choosed.remove(chooser);
                            }
                        }
                    }
                }
            }
            return false;
        });
    }

    protected void outPlayer(MafiaInstance instance, MafiaPlayer player) {
        player.out();
        TextChannel goonChannel = instance.getGoonChannel();
        if (player.getRole() == MafiaRole.GOON && goonChannel != null) {
            Member member = player.getMember();
            if (member != null) {
                PermissionOverride override = goonChannel.getPermissionOverride(player.getMember());
                override.delete().queue();
            }
        }
    }

    public void unpinMessage(MafiaInstance instance) {
        String messageId = (String) instance.removeAttribute(ATTR_MESSAGE_ID + instance.getState().name());
        if (messageId == null) {
            return;
        }
        TextChannel channel = instance.getState() == MafiaState.DAY ? instance.getChannel() : instance.getGoonChannel();
        if (channel != null && channel.getGuild().getSelfMember().hasPermission(channel, Permission.MESSAGE_MANAGE)) {
            channel.unpinMessageById(messageId).queue();
        }
    }

    protected void pinMessage(MafiaInstance instance, Message message) {
        instance.putAttribute(ATTR_MESSAGE_ID + instance.getState().name(), message.getId());
        if (message.getTextChannel().getGuild().getSelfMember().hasPermission(message.getTextChannel(),
                Permission.MESSAGE_MANAGE)) {
            message.getTextChannel().pinMessageById(message.getId()).queue();
        }
    }

    protected abstract String getChoiceKey();

    protected abstract MafiaState getState();
}
