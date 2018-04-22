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
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.caramel.juniperbot.core.listeners.ReactionsListener;
import ru.caramel.juniperbot.core.service.ConfigService;
import ru.caramel.juniperbot.core.service.ContextService;
import ru.caramel.juniperbot.core.service.MessageService;
import ru.caramel.juniperbot.core.support.ModuleListener;
import ru.caramel.juniperbot.module.mafia.model.MafiaInstance;
import ru.caramel.juniperbot.module.mafia.model.MafiaState;
import ru.caramel.juniperbot.module.mafia.service.base.ChoiceStateHandler;
import ru.caramel.juniperbot.module.moderation.service.ModerationService;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class MafiaService implements ModuleListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(MafiaService.class);

    private final static long TIMEOUT = 1800000; // 30 minutes

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

    @Autowired
    private ModerationService moderationService;

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

    public boolean stop(Member requestedBy, TextChannel channel) {
        MafiaInstance instance = getRelatedInstance(channel.getIdLong());
        if (instance == null || !(instance.getState().equals(MafiaState.CHOOSING)
                || instance.isPlayer(requestedBy)
                || moderationService.isModerator(requestedBy))) {
            return false;
        }
        stop(instance);
        return true;
    }

    public void stop(Guild guild) {
        Set<MafiaInstance> guildInstances = instances.values().stream()
                .filter(e -> Objects.equals(guild, e.getGuild())).collect(Collectors.toSet());
        guildInstances.forEach(this::stop);
    }

    public void stop(MafiaInstance instance) {
        instance.stop();
        instance.setState(MafiaState.FINISH);
        if (instance.getGuild().isAvailable() && !MafiaInstance.IGNORED_REASON.equals(instance.getEndReason())) {
            String stopReason = StringUtils.isNotEmpty(instance.getEndReason())
                    ? instance.getEndReason()
                    : messageService.getMessage("mafia.stop.message");

            EmbedBuilder builder = messageService.getBaseEmbed();
            builder.setTitle(messageService.getMessage("mafia.name"));
            builder.setDescription(stopReason);
            try {
                instance.getChannel().sendMessage(builder.build()).queue();
            } catch (Exception e) {
                LOGGER.warn("Cannot send end message", e);
            }
        }

        if (instance.getHandler() != null && instance.getHandler() instanceof ChoiceStateHandler) {
            ((ChoiceStateHandler) instance.getHandler()).unpinMessage(instance);
        }

        if (instance.getGoonChannel() != null) {
            if (instance.getGuild().isAvailable()) {
                try {
                    instance.getGoonChannel().delete().queue();
                } catch (Exception e) {
                    LOGGER.warn("Cannot delete goon channel", e);
                }
            }
            instances.remove(instance.getGoonChannel().getIdLong());
        }
        if (CollectionUtils.isNotEmpty(instance.getListenedMessages())) {
            reactionsListener.unsubscribeAll(instance.getListenedMessages());
        }
        instances.remove(instance.getChannel().getIdLong());
    }

    public MafiaInstance getRelatedInstance(long channelId) {
        MafiaInstance instance = instances.get(channelId);
        if (instance == null) {
            instance = instances.values().stream()
                    .filter(e -> e.getGoonChannel() != null && e.getGoonChannel().getIdLong() == channelId)
                    .findFirst()
                    .orElse(null);
            if (instance != null) {
                instances.put(channelId, instance); // cache goon channel
            }
        }
        return instance;
    }

    public boolean done(User user, TextChannel guild) {
        MafiaInstance instance = getRelatedInstance(guild.getIdLong());
        if (instance == null || !instance.isPlayer(user)) {
            return false;
        }
        if (instance.done(user)) {
            stop(instance);
        }
        return true;
    }

    @Override
    public void onShutdown() {
        instances.values().forEach(e -> {
            try {
                contextService.withContext(e.getGuild(), () -> stop(e));
            } catch (Exception ex) {
                LOGGER.error("Could not stop correctly mafia {}", e.getChannel(), ex);
            }
        });
    }

    @Scheduled(fixedDelay = 15000)
    public void monitor() {
        long currentTimeMillis = System.currentTimeMillis();
        new HashMap<>(this.instances).forEach((k, v) -> {
            if (currentTimeMillis - v.getActiveTime() > TIMEOUT) {
                if (!v.isInState(MafiaState.FINISH)) {
                    contextService.withContext(v.getGuild(),
                            () -> v.setEndReason(messageService.getMessage("mafia.stop.inactive.message")));
                    stop(v);
                }
            }
        });
    }
}
