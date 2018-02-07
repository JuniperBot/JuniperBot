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
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.caramel.juniperbot.module.mafia.model.*;
import ru.caramel.juniperbot.module.mafia.service.base.ChoiceStateHandler;

import java.util.*;

@Component
public class DayHandler extends ChoiceStateHandler {

    private final static String ATTR_CHOICES = "DayHandler.Choices";

    @Autowired
    private GoonHandler goonHandler;

    @Override
    public boolean onStart(User user, MafiaInstance instance) {
        instance.setState(MafiaState.DAY);
        Map<MafiaActionType, MafiaPlayer> actions = instance.getDailyActions();

        MafiaPlayer damagedPlayer = actions.get(MafiaActionType.BROKER_DAMAGE);
        MafiaPlayer healedPlayer = actions.get(MafiaActionType.DOCTOR_HEAL);
        MafiaPlayer killedPlayer = actions.get(MafiaActionType.KILL);

        StringBuilder builder = new StringBuilder();
        if (killedPlayer != null) {
            String roleName = messageService.getEnumTitle(killedPlayer.getRole());
            builder.append(messageService.getMessage("mafia.day.killed",
                    roleName, killedPlayer.getName()));
            if (killedPlayer.equals(healedPlayer)) {
                healedPlayer = null;
            }
            if (killedPlayer.equals(damagedPlayer)) {
                damagedPlayer = null;
            }
            outPlayer(instance, killedPlayer);
        }
        if (damagedPlayer != null) {
            if (builder.length() > 0) {
                builder.append("\n\n");
            }
            int health = damagedPlayer.damage();
            if (health > 0) {
                builder.append(messageService.getMessage("mafia.day.damaged.1", damagedPlayer.getAsMention()));
            } else {
                builder.append(messageService.getMessage("mafia.day.damaged.0", damagedPlayer.getAsMention()));
            }
            if (damagedPlayer.equals(healedPlayer)) {
                damagedPlayer.heal();
                healedPlayer = null;
                builder.append(" ").append(messageService.getMessage("mafia.day.damaged.1.healed"));
                if (health == 0) {
                    builder.append(" ").append(messageService.getMessage("mafia.day.damaged.0.healed"));
                }
            } else if (health == 0) {
                String roleName = messageService.getEnumTitle(damagedPlayer.getRole());
                builder.append(messageService.getMessage("mafia.day.damaged.end", roleName, damagedPlayer.getAsMention()));
                outPlayer(instance, damagedPlayer);
            }
        }
        if (healedPlayer != null) {
            if (builder.length() > 0) {
                builder.append("\n\n");
            }
            builder.append(messageService.getMessage("mafia.day.healed", healedPlayer.getAsMention()));
            if (healedPlayer.isHealthy()) {
                builder.append(" ").append(messageService.getMessage("mafia.day.healed.full", healedPlayer.getAsMention()));
            } else {
                healedPlayer.heal();
            }
        }

        boolean endOfGame = false;
        String message;
        if (builder.length() > 0) {
            boolean hasAnyMafia = instance.hasAnyMafia();
            boolean hasAnyTownie = instance.hasAnyTownie();
            if (!hasAnyMafia && !hasAnyTownie) {
                builder.append("\n\n").append(messageService.getMessage("mafia.end.standoff"));
                endOfGame = true;
            } else if (!hasAnyMafia) {
                builder.append("\n\n").append(messageService.getMessage("mafia.end.townies-wins"));
                endOfGame = true;
            } else if (!hasAnyTownie) {
                builder.append("\n\n").append(messageService.getMessage("mafia.end.mafia-wins"));
                endOfGame = true;
            }
            message = messageService.getMessage("mafia.day.start") + "\n\n" + builder.toString();
        } else {
            message = messageService.getMessage("mafia.day.start.nothing");
        }
        if (!endOfGame) {
            message += "\n\n" + messageService.getMessage("mafia.day.exile.choice");
        }
        EmbedBuilder embedBuilder = getBaseEmbed();
        embedBuilder.setDescription(message);
        if (!endOfGame) {
            embedBuilder.addField(messageService.getMessage("mafia.start.playerList.title"),
                    getPlayerList(instance.getAlive()), false);
            embedBuilder.setFooter(messageService.getMessage("mafia.day.start.footer",
                    getEndTimeText(instance, dayDelay), instance.getPrefix()), null);
        } else {
            instance.setIgnoredReason();
        }
        instance.getDailyActions().clear();
        Message resultMessage = instance.getChannel().sendMessage(embedBuilder.build()).complete();

        if (!endOfGame) {
            sendChoice(instance, resultMessage, instance.getAlive());
        }
        return endOfGame || scheduleEnd(instance, dayDelay);
    }

    @Override
    public boolean onEnd(User user, MafiaInstance instance) {
        MafiaPlayer toExile = getChoiceResult(instance);
        if (toExile != null) {
            instance.getDailyActions().put(MafiaActionType.EXILE, toExile);
        }
        String messageId = (String) instance.removeAttribute(ATTR_MESSAGE_ID);
        if (messageId != null && instance.getChannel().getGuild().getSelfMember().hasPermission(instance.getChannel(),
                Permission.MESSAGE_MANAGE)) {
            instance.getChannel().unpinMessageById(messageId).queue();
        }
        return goonHandler.onStart(user, instance);
    }

    @Override
    protected String getChoiceKey() {
        return "DayHandler.Choices";
    }

    @Override
    protected MafiaState getState() {
        return MafiaState.DAY;
    }
}
