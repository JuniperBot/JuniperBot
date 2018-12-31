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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.caramel.juniperbot.core.service.DiscordService;
import ru.caramel.juniperbot.module.audio.model.LavaLinkConfiguration;
import ru.caramel.juniperbot.module.audio.service.LavaAudioService;
import ru.caramel.juniperbot.module.audio.utils.GuildAudioSendHandler;

import java.net.URI;
import java.net.URISyntaxException;

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

    @Getter
    private JdaLavalink lavaLink = null;

    @Override
    public void configure(DiscordService discordService, DefaultShardManagerBuilder builder) {
        if (jdaNAS) {
            builder.setAudioSendFactory(new NativeAudioSendFactory());
        }
        if (configuration.isEnabled()) {
            if (CollectionUtils.isNotEmpty(configuration.getNodes())) {
                lavaLink = new JdaLavalink(
                        discordService.getUserId(),
                        discordService.getShardsNum(),
                        discordService::getShardById
                );
                configuration.getNodes().forEach(e -> {
                    try {
                        lavaLink.addNode(e.getName(), new URI(e.getUrl()), e.getPassword());
                    } catch (URISyntaxException e1) {
                        log.warn("Could not add node {}", e, e1);
                    }
                });
                builder.addEventListeners(lavaLink);
            } else {
                log.warn("Lavalink is enabled but no valid nodes was specified");
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
