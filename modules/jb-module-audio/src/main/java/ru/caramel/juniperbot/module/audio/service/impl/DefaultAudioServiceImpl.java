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
package ru.caramel.juniperbot.module.audio.service.impl;

import com.sedmelluq.discord.lavaplayer.jdaudp.NativeAudioSendFactory;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import lavalink.client.io.Link;
import lavalink.client.io.jda.JdaLavalink;
import lavalink.client.player.IPlayer;
import lavalink.client.player.LavaplayerPlayerWrapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.bot.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.managers.AudioManager;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import ru.caramel.juniperbot.core.configuration.SchedulerConfiguration;
import ru.caramel.juniperbot.core.service.DiscordService;
import ru.caramel.juniperbot.module.audio.model.LavaLinkConfiguration;
import ru.caramel.juniperbot.module.audio.service.LavaAudioService;
import ru.caramel.juniperbot.module.audio.utils.GuildAudioSendHandler;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class DefaultAudioServiceImpl implements LavaAudioService {

    @Value("${discord.audio.engine.jdaNAS:true}")
    private boolean jdaNAS;

    @Autowired
    @Getter
    private LavaLinkConfiguration configuration;

    @Autowired
    private AudioPlayerManager playerManager;

    @Autowired
    private DiscoveryClient discoveryClient;

    @Autowired
    @Qualifier(SchedulerConfiguration.COMMON_SCHEDULER_NAME)
    private TaskScheduler scheduler;

    @Getter
    private JdaLavalink lavaLink = null;

    private Set<URI> registeredInstances = Collections.synchronizedSet(new HashSet<>());

    @Override
    public void configure(DiscordService discordService, DefaultShardManagerBuilder builder) {
        if (jdaNAS) {
            builder.setAudioSendFactory(new NativeAudioSendFactory());
        }
        if (configuration.isEnabled()) {
            lavaLink = new JdaLavalink(
                    discordService.getUserId(),
                    discordService.getShardsNum(),
                    discordService::getShardById
            );
            builder.addEventListeners(lavaLink);
            if (CollectionUtils.isNotEmpty(configuration.getNodes())) {
                configuration.getNodes().forEach(e -> {
                    try {
                        lavaLink.addNode(e.getName(), new URI(e.getUrl()), e.getPassword());
                    } catch (URISyntaxException e1) {
                        log.warn("Could not add node {}", e, e1);
                    }
                });
            }

            var discovery = configuration.getDiscovery();
            if (discovery != null && discovery.isEnabled() && StringUtils.isNotEmpty(discovery.getServiceName())) {
                scheduler.scheduleWithFixedDelay(this::lookUpDiscovery, 60000);
            }
        }
    }

    @Override
    public IPlayer createPlayer(String guildId) {
        return lavaLink != null
                ? lavaLink.getLink(guildId).getPlayer()
                : new LavaplayerPlayerWrapper(playerManager.createPlayer());
    }

    @Override
    public void openConnection(IPlayer player, VoiceChannel channel) {
        if (lavaLink != null) {
            lavaLink.getLink(channel.getGuild()).connect(channel);
        } else {
            AudioManager audioManager = channel.getGuild().getAudioManager();
            if (audioManager.getConnectedChannel() == null) {
                audioManager.setSendingHandler(new GuildAudioSendHandler(player));
            }
            channel.getGuild().getAudioManager().openAudioConnection(channel);
        }
    }

    @Override
    public void closeConnection(Guild guild) {
        if (lavaLink != null) {
            // use destroy here for guild instead of simple disconnect bacause
            lavaLink.getLink(guild).destroy();
        } else {
            guild.getAudioManager().closeAudioConnection();
        }
    }

    @Override
    public void shutdown() {
        if (lavaLink != null) {
            lavaLink.shutdown();
        }
        playerManager.shutdown();
    }

    private void lookUpDiscovery() {
        var discovery = configuration.getDiscovery();
        if (discovery == null || !discovery.isEnabled() || StringUtils.isEmpty(discovery.getServiceName())) {
            return;
        }

        try {
            List<ServiceInstance> instanceList = discoveryClient.getInstances(discovery.getServiceName());
            for (ServiceInstance instance : instanceList) {
                try {
                    URI uri = new URI(String.format("ws://%s:%s", instance.getHost(), instance.getPort()));
                    if (registeredInstances.add(uri)) {
                        lavaLink.addNode(instance.getInstanceId(), uri, discovery.getPassword());
                    }
                } catch (URISyntaxException e) {
                    log.warn("Could not add node {}", instance, e);
                }
            }
        } catch (Exception e) {
            log.warn("Could not initialize Lavalink services", e);
        }
    }

    @Override
    public boolean isConnected(Guild guild) {
        if (lavaLink != null) {
            Link link = lavaLink.getLink(guild);
            return link.getState() == Link.State.CONNECTED || link.getState() == Link.State.CONNECTING;
        } else {
            AudioManager audioManager = guild.getAudioManager();
            return audioManager != null && (audioManager.isConnected() || audioManager.isAttemptingToConnect());
        }
    }
}
