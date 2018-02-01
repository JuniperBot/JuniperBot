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
import net.dv8tion.jda.core.entities.PrivateChannel;
import net.dv8tion.jda.core.entities.User;
import org.apache.commons.lang3.ArrayUtils;
import ru.caramel.juniperbot.core.listeners.ReactionsListener;
import ru.caramel.juniperbot.module.mafia.model.MafiaInstance;
import ru.caramel.juniperbot.module.mafia.model.MafiaPlayer;
import ru.caramel.juniperbot.module.mafia.model.MafiaRole;
import ru.caramel.juniperbot.module.mafia.model.MafiaState;

import java.util.*;

public abstract class IndividualHandler<T extends MafiaStateHandler> extends AbstractStateHandler {

    protected final MafiaRole individualRole;

    protected final MafiaState state;

    protected final Set<MafiaState> choiceStates;

    private T nextHandler;

    protected IndividualHandler(MafiaRole individualRole, MafiaState state, MafiaState... choiceStates) {
        this.individualRole = individualRole;
        this.state = state;
        this.choiceStates = new HashSet<>();
        this.choiceStates.add(state);
        if (choiceStates != null) {
            this.choiceStates.addAll(Arrays.asList(choiceStates));
        }
    }

    @Override
    public boolean onStart(User user, MafiaInstance instance) {
        MafiaPlayer individual = instance.getPlayerByRole(individualRole);
        if (individual == null) {
            return getNextHandler().onStart(user, instance);
        }
        MafiaState oldState = instance.updateState(state);
        StringBuilder messageBuilder = new StringBuilder();
        if (oldState.getTransitionEnd() != null) {
            messageBuilder.append(messageService.getMessage(oldState.getTransitionEnd())).append(" ");
        }
        if (state.getTransitionStart() != null) {
            messageBuilder.append(messageService.getMessage(state.getTransitionStart()));
        }
        if (messageBuilder.length() > 0) {
            EmbedBuilder builder = getBaseEmbed();
            builder.setDescription(messageBuilder.toString());
            instance.getChannel().sendMessage(builder.build()).complete();
        }
        if (!sendChoiceMessage(instance, individualRole.getChoiceMessage())) {
            instance.setEndReason(messageService.getMessage("mafia.end.reason.couldNotDM",
                    individual.getMember().getEffectiveName()));
            return true;
        }
        return scheduleEnd(instance, individualDelay);
    }

    @Override
    public boolean onEnd(User user, MafiaInstance instance) {
        return (user == null || instance.isPlayer(user, individualRole)) && getNextHandler().onStart(user, instance);
    }

    public boolean sendChoiceMessage(MafiaInstance instance, String welcomeCode) {
        return sendChoiceMessage(instance, welcomeCode, null, true);
    }

    public boolean sendChoiceMessage(MafiaInstance instance, String welcomeCode, Long endDelay, boolean pass) {
        MafiaPlayer individual = instance.getPlayerByRole(individualRole);
        if (individual == null) {
            return false;
        }

        EmbedBuilder builder = getBaseEmbed(welcomeCode);
        builder.setFooter(messageService.getMessage("mafia.invidual.choice.footer", getEndTimeText(instance,
                endDelay != null ? endDelay : individualDelay)), null);
        List<MafiaPlayer> players = new ArrayList<>(instance.getAlive());
        players.remove(individual);
        if (!players.isEmpty()) {
            builder.addField(messageService.getMessage("mafia.start.playerList.title"),
                    getPlayerList(players), false);
        }

        PrivateChannel channel = openPrivateChannel(individual);
        if (channel == null) {
            return false;
        }

        Message message = channel.sendMessage(builder.build()).complete();
        try {
            for (int i = 0; i < players.size(); i++) {
                message.addReaction(ReactionsListener.CHOICES[i]).submit();
            }
        } catch (Exception ex) {
            // ignore
        }
        instance.getListenedMessages().add(message.getId());
        reactionsListener.onReactionAdd(message.getId(), event -> {
            if (!event.getUser().equals(event.getJDA().getSelfUser()) && choiceStates.contains(instance.getState())) {
                String emote = event.getReaction().getReactionEmote().getName();
                int index = ArrayUtils.indexOf(ReactionsListener.CHOICES, emote);
                if (index >= 0 && index < players.size()) {
                    MafiaPlayer player = players.get(index);
                    contextService.withContext(instance.getGuild(), () -> {
                        if (player != null) {
                            instance.tick();
                            choiceAction(instance, player, channel);
                        }
                        if (pass) {
                            instance.done(event.getUser());
                        }
                    });
                    return true;
                }
            }
            return false;
        });
        return true;

    }

    protected synchronized T getNextHandler() {
        if (nextHandler == null) {
            nextHandler = getHandler(getNextType());
        }
        return nextHandler;
    }

    protected abstract Class<T> getNextType();

    protected abstract void choiceAction(MafiaInstance instance, MafiaPlayer target, PrivateChannel channel);

}
