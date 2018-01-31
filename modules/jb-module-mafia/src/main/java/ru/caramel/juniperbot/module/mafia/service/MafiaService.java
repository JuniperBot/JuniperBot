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
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.caramel.juniperbot.core.listeners.ReactionsListener;
import ru.caramel.juniperbot.core.service.ConfigService;
import ru.caramel.juniperbot.core.service.ContextService;
import ru.caramel.juniperbot.core.service.MessageService;
import ru.caramel.juniperbot.core.support.ModuleListener;
import ru.caramel.juniperbot.module.mafia.model.MafiaInstance;
import ru.caramel.juniperbot.module.mafia.model.MafiaState;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MafiaService implements ModuleListener {

    @Autowired
    private ChoosingHandler choosingHandler;

    @Autowired
    private ContextService contextService;

    @Autowired
    private ConfigService configService;

    @Autowired
    private ReactionsListener reactionsListener;

    @Autowired
    private MessageService messageService;

    private final Map<Long, MafiaInstance> instances = new ConcurrentHashMap<>();

    public MafiaInstance getInstance(TextChannel channel) {
        return instances.computeIfAbsent(channel.getIdLong(), e -> new MafiaInstance(
                channel,
                contextService.getLocale(),
                configService.getPrefix(channel.getGuild().getIdLong())));
    }

    public boolean start(User user, TextChannel guild) {
        if (instances.containsKey(guild.getIdLong())) {
            return false;
        }
        MafiaInstance instance = getInstance(guild);
        return !choosingHandler.onStart(user, instance);
    }

    public boolean stop(TextChannel channel) {
        MafiaInstance instance = getRelatedInstance(channel.getIdLong());
        if (instance != null) {
            stop(instance, true);
            return true;
        }
        return false;
    }

    public void stop(MafiaInstance instance) {
        instance.setState(MafiaState.FINISH);

        if (!MafiaInstance.IGNORED_REASON.equals(instance.getEndReason())) {
            String stopReason = StringUtils.isNotEmpty(instance.getEndReason())
                    ? instance.getEndReason()
                    : messageService.getMessage("mafia.stop.message");

            EmbedBuilder builder = messageService.getBaseEmbed();
            builder.setTitle(messageService.getMessage("mafia.name"));
            builder.setDescription(stopReason);
            instance.getChannel().sendMessage(builder.build()).complete();
        }

        if (instance.getGoonChannel() != null) {
            instance.getGoonChannel().delete().complete();
        }
        if (CollectionUtils.isNotEmpty(instance.getListenedMessages())) {
            reactionsListener.unsubscribeAll(instance.getListenedMessages());
        }
        instances.remove(instance.getGuild().getIdLong());
    }

    public void stop(MafiaInstance instance, boolean cancelScheduled) {
        if (cancelScheduled) {
            instance.stop();
        }
        stop(instance);
    }

    private MafiaInstance getRelatedInstance(long channelId) {
        MafiaInstance instance = instances.get(channelId);
        if (instance == null) {
            instance = instances.values().stream()
                    .filter(e -> e.getGoonChannel() != null && e.getGoonChannel().getIdLong() == channelId)
                    .findFirst()
                    .orElse(null);
        }
        return instance;
    }

    public boolean done(User user, TextChannel guild) {
        MafiaInstance instance = getRelatedInstance(guild.getIdLong());
        if (instance == null || !instance.isPlayer(user)) {
            return false;
        }
        if (instance.done(user)) {
            stop(instance, true);
        }
        return true;
    }

    @Override
    public void onShutdown() {
        instances.values().forEach(e -> {
            contextService.initContext(e.getGuild());
            stop(e, true);
            contextService.resetContext();
        });
    }
}
