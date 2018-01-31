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
import net.dv8tion.jda.core.entities.PrivateChannel;
import net.dv8tion.jda.core.entities.User;
import org.apache.commons.lang3.StringUtils;
import org.ocpsoft.prettytime.PrettyTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.TaskScheduler;
import ru.caramel.juniperbot.core.listeners.ReactionsListener;
import ru.caramel.juniperbot.core.service.ContextService;
import ru.caramel.juniperbot.core.service.DiscordService;
import ru.caramel.juniperbot.core.service.MessageService;
import ru.caramel.juniperbot.module.mafia.model.MafiaInstance;
import ru.caramel.juniperbot.module.mafia.model.MafiaPlayer;
import ru.caramel.juniperbot.module.mafia.model.MafiaState;

import java.util.Collection;
import java.util.Date;

public abstract class AbstractStateHandler implements MafiaStateHandler {

    @Value("${mafia.debug:false}")
    protected boolean debug;

    @Value("${mafia.day.delay:600000}")
    protected Long dayDelay;

    @Value("${mafia.choosing.delay:180000}")
    protected Long choosingDelay;

    @Value("${mafia.meeting.delay:30000}")
    protected Long meetingDelay;

    @Value("${mafia.individual.delay:60000}")
    protected Long individualDelay;

    @Autowired
    protected TaskScheduler scheduler;

    @Autowired
    protected MessageService messageService;

    @Autowired
    protected ReactionsListener reactionsListener;

    @Autowired
    protected MafiaService mafiaService;

    @Autowired
    protected ContextService contextService;

    @Autowired
    protected ApplicationContext context;

    @Autowired
    protected DiscordService discordService;

    protected EmbedBuilder getBaseEmbed() {
        EmbedBuilder embed = messageService.getBaseEmbed();
        embed.setTitle(messageService.getMessage("mafia.name"));
        return embed;
    }

    protected EmbedBuilder getBaseEmbed(String code, Object... args) {
        EmbedBuilder embed = getBaseEmbed();
        if (StringUtils.isNotEmpty(code)) {
            embed.setDescription(messageService.getMessage(code, args));
        }
        return embed;
    }

    protected boolean sendMessage(MafiaPlayer player, String code) {
        if (player != null) {
            PrivateChannel channel = openPrivateChannel(player);
            if (channel == null) {
                return false;
            }
            channel.sendMessage(messageService.getMessage(code)).complete();
        }
        return true;
    }

    protected boolean scheduleEnd(MafiaInstance instance, long delay) {
        if (MafiaState.FINISH.equals(instance.getState())) {
            return true;
        }
        Date date = new Date();
        date.setTime(date.getTime() + delay);
        instance.setStepEndTime(date);
        instance.setScheduledStep(scheduler.schedule(() -> {
            if (!MafiaState.FINISH.equals(instance.getState())) {
                contextService.initContext(instance.getGuild());
                if (onEnd(null, instance)) {
                    mafiaService.stop(instance);
                }
                contextService.resetContext();
            }
        }, date));
        instance.setHandler(this);
        return false;
    }

    protected String getEndTimeText(MafiaInstance instance, long delay) {
        Date date = new Date();
        date.setTime(date.getTime() + delay);
        return new PrettyTime(instance.getLocale()).format(date);
    }

    protected String getPlayerList(Collection<MafiaPlayer> players) {
        int counter = 0;
        StringBuilder builder = new StringBuilder();
        for (MafiaPlayer player : players) {
            builder
                    .append(counter > 0 ? "\n" : "")
                    .append(++counter)
                    .append(". ")
                    .append(player.getMember().getEffectiveName());
        }
        return builder.toString();
    }

    protected PrivateChannel openPrivateChannel(MafiaPlayer player) {
        User user = discordService.getJda().getUserById(player.getUser().getId());
        try {
            return user.openPrivateChannel().complete();
        } catch (Exception e) {
            return null;
        }
    }

    protected <T extends MafiaStateHandler> T getHandler(Class<T> handler) {
        return context.getBean(handler);
    }
}
