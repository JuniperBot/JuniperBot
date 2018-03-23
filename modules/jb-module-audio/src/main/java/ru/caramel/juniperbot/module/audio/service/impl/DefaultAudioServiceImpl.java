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
import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.bandcamp.BandcampAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.beam.BeamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.vimeo.VimeoAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import lavalink.client.io.Lavalink;
import lavalink.client.io.Link;
import lavalink.client.player.IPlayer;
import lavalink.client.player.LavaplayerPlayerWrapper;
import lombok.Getter;
import net.dv8tion.jda.bot.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.core.audio.factory.IAudioSendFactory;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.managers.AudioManager;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.caramel.juniperbot.core.service.DiscordService;
import ru.caramel.juniperbot.module.audio.service.LavaAudioService;
import ru.caramel.juniperbot.module.audio.utils.GuildAudioSendHandler;

import javax.annotation.PostConstruct;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Stream;

@Service
public class DefaultAudioServiceImpl implements LavaAudioService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultAudioServiceImpl.class);

    @Value("${discord.audio.engine.jdaNAS:true}")
    private boolean jdaNAS;

    @Value("${discord.audio.engine.lavaLink.enabled:false}")
    private boolean lavaLinkEnabled;

    private Map<URI, String> lavaLinkNodes;

    @Value("${discord.audio.resamplingQuality:MEDIUM}")
    private AudioConfiguration.ResamplingQuality resamplingQuality;

    @Value("${discord.audio.frameBufferDuration:2000}")
    private int frameBufferDuration;

    @Value("${discord.audio.itemLoaderThreadPoolSize:500}")
    private int itemLoaderThreadPoolSize;

    @Autowired
    private DiscordService discordService;

    @Autowired(required = false)
    private IAudioSendFactory audioSendFactory;

    @Autowired
    private List<AudioSourceManager> audioSourceManagers;

    @Getter
    private AudioPlayerManager playerManager;

    @Getter
    private Lavalink lavaLink = null;

    @Override
    public void configure(DefaultShardManagerBuilder builder) {
        if (jdaNAS) {
            builder.setAudioSendFactory(new NativeAudioSendFactory());
        } else if (audioSendFactory != null) {
            builder.setAudioSendFactory(audioSendFactory);
        }
        if (lavaLinkEnabled) {
            if (MapUtils.isNotEmpty(lavaLinkNodes)) {
                lavaLink = new Lavalink(
                        discordService.getUserId(),
                        discordService.getShardsNum(),
                        discordService::getShardById
                );
                lavaLinkNodes.forEach(lavaLink::addNode);
                builder.addEventListeners(lavaLink);
            } else {
                LOGGER.warn("Lavalink is enabled but no valid nodes was specified");
            }
        }
    }

    @PostConstruct
    public void init() {
        playerManager = new DefaultAudioPlayerManager();
        playerManager.getConfiguration().setResamplingQuality(resamplingQuality);
        playerManager.setFrameBufferDuration(frameBufferDuration);
        playerManager.setItemLoaderThreadPoolSize(itemLoaderThreadPoolSize);
        playerManager.registerSourceManager(new YoutubeAudioSourceManager(true));
        playerManager.registerSourceManager(new SoundCloudAudioSourceManager());
        playerManager.registerSourceManager(new BandcampAudioSourceManager());
        playerManager.registerSourceManager(new VimeoAudioSourceManager());
        playerManager.registerSourceManager(new TwitchStreamAudioSourceManager());
        playerManager.registerSourceManager(new BeamAudioSourceManager());
        if (CollectionUtils.isNotEmpty(audioSourceManagers)) {
            audioSourceManagers.forEach(playerManager::registerSourceManager);
        }
    }


    @Value("${discord.audio.engine.lavaLink.nodes:}")
    public void setLavaLinkNodes(String value) {
        if (StringUtils.isEmpty(value)) {
            return;
        }
        String[] nodes = value.split(";");
        Map<URI, String> nodeMap = new HashMap<>(nodes.length);
        Stream.of(nodes).forEach(e -> {
            String[] node = e.split("\\|");
            if (node.length > 1) {
                try {
                    nodeMap.put(new URI(node[0]), node[1]);
                } catch (URISyntaxException ex) {
                    LOGGER.warn("Could not parse node {}", e, ex);
                }
            }
        });
        lavaLinkNodes = Collections.unmodifiableMap(nodeMap);
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
    public VoiceChannel getConnectedChannel(Guild guild) {
        //NOTE: never use the local audio manager, since the audio connection may be remote
        // there is also no reason to look the channel up remotely from lavalink, if we have access to a real guild
        // object here, since we can use the voice state of ourselves (and lavalink 1.x is buggy in keeping up with the
        // current voice channel if the bot is moved around in the client)
        return guild.getSelfMember().getVoiceState().getChannel();
    }

    @Override
    public VoiceChannel getConnectedChannel(long guildId) {
        return getConnectedChannel(discordService.getShardManager().getGuildById(guildId));
    }

    @Override
    public void shutdown() {
        if (lavaLink != null) {
            lavaLink.shutdown();
        } else {
            playerManager.shutdown();
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
