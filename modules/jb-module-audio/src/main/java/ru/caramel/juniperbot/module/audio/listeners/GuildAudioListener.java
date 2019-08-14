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
package ru.caramel.juniperbot.module.audio.listeners;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.channel.voice.VoiceChannelDeleteEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import ru.caramel.juniperbot.core.event.DiscordEvent;
import ru.caramel.juniperbot.core.event.listeners.DiscordEventListener;
import ru.caramel.juniperbot.core.feature.service.FeatureSetService;
import ru.caramel.juniperbot.module.audio.persistence.entity.MusicConfig;
import ru.caramel.juniperbot.module.audio.service.MusicConfigService;
import ru.caramel.juniperbot.module.audio.service.PlayerService;

import java.util.Objects;

@DiscordEvent
public class GuildAudioListener extends DiscordEventListener {

    @Autowired
    private PlayerService playerService;

    @Autowired
    private MusicConfigService musicConfigService;

    @Autowired
    private FeatureSetService featureSetService;

    @Override
    public void onGuildLeave(GuildLeaveEvent event) {
        playerService.stop(null, event.getGuild());
    }

    @Override
    public void onVoiceChannelDelete(VoiceChannelDeleteEvent event) {
        if (playerService.isActive(event.getGuild()) &&
                Objects.equals(event.getChannel(), playerService.getConnectedChannel(event.getGuild()))) {
            playerService.stop(null, event.getGuild());
        }
    }

    @Override
    public void onGuildVoiceMove(GuildVoiceMoveEvent event) {
        tryAutoPlay(event.getMember(), event.getGuild(), event.getChannelJoined());
    }

    @Override
    public void onGuildVoiceJoin(GuildVoiceJoinEvent event) {
        tryAutoPlay(event.getMember(), event.getGuild(), event.getChannelJoined());
    }

    private void tryAutoPlay(Member member, Guild guild, VoiceChannel joinedChannel) {
        if (member.getUser().isBot()
                || !featureSetService.isAvailable(guild)
                || playerService.isActive(guild)) {
            return;
        }
        contextService.withContextAsync(guild, () -> {
            MusicConfig config = musicConfigService.get(guild);
            if (config == null) {
                return;
            }

            VoiceChannel targetChannel = null;
            if (config.getChannelId() != null) {
                targetChannel = guild.getVoiceChannelById(config.getChannelId());
            }
            if (!hasPermission(targetChannel)) {
                for (VoiceChannel voiceChannel : guild.getVoiceChannels()) {
                    if (hasPermission(voiceChannel)) {
                        targetChannel = voiceChannel;
                        break;
                    }
                }
            }

            if (joinedChannel.equals(targetChannel)
                    && targetChannel.getMembers().size() < 2
                    && StringUtils.isNotEmpty(config.getAutoPlay())) {
                TextChannel channel = null;
                if (config.getTextChannelId() != null) {
                    channel = guild.getTextChannelById(config.getTextChannelId());
                }

                if (!hasPermission(channel)) {
                    for (TextChannel textChannel : guild.getTextChannels()) {
                        if (hasPermission(textChannel)) {
                            channel = textChannel;
                            break;
                        }
                    }
                }

                if (channel != null) {
                    playerService.loadAndPlay(channel, member, config.getAutoPlay());
                }
            }
        });
    }

    private boolean hasPermission(TextChannel channel) {
        return channel != null && channel.getGuild().getSelfMember().hasPermission(channel, Permission.MESSAGE_WRITE,
                Permission.MESSAGE_EMBED_LINKS);
    }

    private boolean hasPermission(VoiceChannel channel) {
        return channel != null && channel.getGuild().getSelfMember().hasPermission(channel, Permission.VOICE_CONNECT,
                Permission.VOICE_SPEAK);
    }
}
