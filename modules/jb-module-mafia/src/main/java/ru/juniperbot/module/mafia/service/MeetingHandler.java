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
package ru.juniperbot.module.mafia.service;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.juniperbot.module.mafia.model.MafiaInstance;
import ru.juniperbot.module.mafia.model.MafiaPlayer;
import ru.juniperbot.module.mafia.model.MafiaState;
import ru.juniperbot.module.mafia.service.base.AbstractStateHandler;
import ru.juniperbot.module.mafia.service.individual.CopHandler;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

@Component
public class MeetingHandler extends AbstractStateHandler {

    @Autowired
    private CopHandler copHandler;

    @Autowired
    private DayHandler dayHandler;

    @Override
    public boolean onStart(User user, MafiaInstance instance) {
        instance.tick();
        instance.setState(MafiaState.MEETING);

        EmbedBuilder builder = getBaseEmbed("mafia.meeting.welcome");
        builder.addField(messageService.getMessage("mafia.start.playerList.title"),
                getPlayerList(instance.getPlayers()), false);
        if (MEETING_DELAY >= 60000) {
            builder.setFooter(messageService.getMessage("mafia.meeting.welcome.footer",
                    getEndTimeText(instance, MEETING_DELAY)), null);
        }

        TextChannel channel = instance.getChannel();
        if (channel == null) {
            return true; // end for non existent channel instantly
        }

        channel.sendMessage(builder.build()).queue();
        createGoonChannel(instance, instance::setGoonChannel);

        if (!sendMessage(instance.getDoctor(), "mafia.meeting.doctor.welcome")) {
            instance.setEndReason(messageService.getMessage("mafia.end.reason.couldNotDM",
                    instance.getDoctor().getName()));
            return true;
        }

        if (!sendMessage(instance.getBroker(), "mafia.meeting.broker.welcome")) {
            instance.setEndReason(messageService.getMessage("mafia.end.reason.couldNotDM",
                    instance.getBroker().getName()));
            return true;
        }
        if (instance.getCop() != null) {
            if (!copHandler.sendChoiceMessage(instance, "mafia.meeting.cop.welcome", MEETING_DELAY, false)) {
                instance.setEndReason(messageService.getMessage("mafia.end.reason.couldNotDM",
                        instance.getCop().getName()));
                return true;
            }
        }
        return scheduleEnd(instance, MEETING_DELAY);
    }

    @Override
    public boolean onEnd(User user, MafiaInstance instance) {
        return user == null && dayHandler.onStart(null, instance);
    }

    private void createGoonChannel(MafiaInstance instance, Consumer<TextChannel> queue) {
        Guild guild = instance.getGuild();
        Role everyOne = guild.getPublicRole();

        List<Permission> permissionList = Arrays.asList(Permission.VIEW_CHANNEL, Permission.MESSAGE_WRITE,
                Permission.MESSAGE_ADD_REACTION);

        ChannelAction<TextChannel> action = guild
                .createTextChannel("wolves")
                .setTopic(messageService.getMessage("mafia.goons"))
                .addPermissionOverride(everyOne, null, permissionList)
                .addPermissionOverride(guild.getSelfMember(), permissionList, null);
        for (MafiaPlayer goon : instance.getGoons()) {
            Member member = goon.getMember();
            if (member != null) {
                action.addPermissionOverride(member, permissionList, null);
            }
        }

        contextService.queue(instance.getGuildId(), action, channel -> {
            String message = messageService.getMessage("mafia.meeting.goons.welcome", instance.getGoonsMentions());
            channel.sendMessage(message).queue();
            queue.accept(channel);
        });
    }
}
