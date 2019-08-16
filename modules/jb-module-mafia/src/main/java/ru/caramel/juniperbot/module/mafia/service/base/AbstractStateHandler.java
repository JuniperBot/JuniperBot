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

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.PrivateChannel;
import org.apache.commons.lang3.StringUtils;
import org.ocpsoft.prettytime.PrettyTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.TaskScheduler;
import ru.caramel.juniperbot.core.common.service.ConfigService;
import ru.caramel.juniperbot.core.common.service.DiscordService;
import ru.caramel.juniperbot.core.configuration.SchedulerConfiguration;
import ru.caramel.juniperbot.core.event.listeners.ReactionsListener;
import ru.caramel.juniperbot.core.event.service.ContextService;
import ru.caramel.juniperbot.core.message.service.MessageService;
import ru.caramel.juniperbot.module.mafia.model.MafiaInstance;
import ru.caramel.juniperbot.module.mafia.model.MafiaPlayer;
import ru.caramel.juniperbot.module.mafia.model.MafiaState;
import ru.caramel.juniperbot.module.mafia.service.MafiaService;

import java.util.Collection;
import java.util.Date;
import java.util.function.Consumer;

public abstract class AbstractStateHandler implements MafiaStateHandler {

    protected static final String CHOOSE = "✅";

    @Value("${features.mafia.debug:false}")
    protected boolean debug;

    @Value("${features.mafia.dayDelay:300000}")
    protected Long dayDelay;

    @Value("${features.mafia.choosingDelay:300000}")
    protected Long choosingDelay;

    @Value("${features.mafia.meetingDelay:30000}")
    protected Long meetingDelay;

    @Value("${features.mafia.individualDelay:120000}")
    protected Long individualDelay;

    @Autowired
    @Qualifier(SchedulerConfiguration.COMMON_SCHEDULER_NAME)
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

    @Autowired
    protected ConfigService configService;

    protected EmbedBuilder getBaseEmbed() {
        EmbedBuilder embed = messageService.getBaseEmbed();
        embed.setTitle(messageService.getMessage("discord.command.group.mafia"));
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
            return openPrivateChannel(player, channel -> channel.sendMessage(messageService.getMessage(code)).queue());
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
                contextService.withContext(instance.getGuild(), () -> {
                    if (!instance.getGuild().isAvailable() || onEnd(null, instance)) {
                        mafiaService.stop(instance);
                    }
                });
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
                    .append(player.getName());
        }
        return builder.toString();
    }

    protected boolean openPrivateChannel(MafiaPlayer player, Consumer<PrivateChannel> queue) {
        try {
            contextService.queue(player.getGuildId(), player.getUser().openPrivateChannel(), queue);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    protected <T extends MafiaStateHandler> T getHandler(Class<T> handler) {
        return context.getBean(handler);
    }
}
