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

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.juniperbot.common.persistence.entity.GuildConfig;
import ru.caramel.juniperbot.module.mafia.model.MafiaActionType;
import ru.caramel.juniperbot.module.mafia.model.MafiaInstance;
import ru.caramel.juniperbot.module.mafia.model.MafiaPlayer;
import ru.caramel.juniperbot.module.mafia.model.MafiaState;
import ru.caramel.juniperbot.module.mafia.service.base.ChoiceStateHandler;

import java.util.Map;
import java.util.function.Consumer;

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
            GuildConfig config = configService.get(instance.getGuild());
            String nextCommand = messageService.getMessageByLocale("discord.command.mafia.done.key", config.getCommandLocale());

            embedBuilder.addField(messageService.getMessage("mafia.start.playerList.title"),
                    getPlayerList(instance.getAlive()), false);
            embedBuilder.setFooter(messageService.getMessage("mafia.day.start.footer",
                    getEndTimeText(instance, dayDelay), instance.getPrefix(), nextCommand), null);
        } else {
            instance.setIgnoredReason();
        }
        instance.getDailyActions().clear();

        TextChannel channel = instance.getChannel();
        if (channel == null) {
            return true; // end for non existent channel instantly
        }

        Consumer<? super Message> onMessage = null;
        if (!endOfGame) {
            onMessage = m -> {
                sendChoice(instance, m, instance.getAlive());
            };
        }
        channel.sendMessage(embedBuilder.build()).queue(onMessage);

        return endOfGame || scheduleEnd(instance, dayDelay);
    }

    @Override
    public boolean onEnd(User user, MafiaInstance instance) {
        MafiaPlayer toExile = getChoiceResult(instance);
        if (toExile != null) {
            instance.getDailyActions().put(MafiaActionType.EXILE, toExile);
        }
        unpinMessage(instance);
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
