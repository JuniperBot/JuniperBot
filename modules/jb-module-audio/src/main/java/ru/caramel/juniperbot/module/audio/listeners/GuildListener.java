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

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceJoinEvent;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.caramel.juniperbot.core.listeners.DiscordEventListener;
import ru.caramel.juniperbot.module.audio.persistence.entity.MusicConfig;
import ru.caramel.juniperbot.module.audio.service.MusicConfigService;
import ru.caramel.juniperbot.module.audio.service.PlayerService;

@Component
public class GuildListener extends DiscordEventListener {

    @Autowired
    private PlayerService playerService;

    @Autowired
    private MusicConfigService musicConfigService;

    @Override
    public void onGuildLeave(GuildLeaveEvent event) {
        playerService.stop(null, event.getGuild());
    }

    @Override
    public void onGuildVoiceJoin(GuildVoiceJoinEvent event) {
        if (playerService.isActive(event.getGuild())) {
            return;
        }

        MusicConfig config = musicConfigService.getConfig(event.getGuild());

        VoiceChannel targetChannel = null;
        if (config.getChannelId() != null) {
            targetChannel = event.getGuild().getVoiceChannelById(config.getChannelId());
        }
        if (!hasPermission(targetChannel)) {
            for (VoiceChannel voiceChannel : event.getGuild().getVoiceChannels()) {
                if (hasPermission(voiceChannel)) {
                    targetChannel = voiceChannel;
                    break;
                }
            }
        }

        if (event.getChannelJoined().equals(targetChannel) && StringUtils.isNotEmpty(config.getAutoPlay())) {
            TextChannel channel = null;
            if (config.getTextChannelId() != null) {
                channel = event.getGuild().getTextChannelById(config.getTextChannelId());
            }

            if (!hasPermission(channel)) {
                for (TextChannel textChannel : event.getGuild().getTextChannels()) {
                    if (hasPermission(textChannel)) {
                        channel = textChannel;
                        break;
                    }
                }
            }

            if (channel != null) {
                playerService.loadAndPlay(channel, event.getMember(), config.getAutoPlay());
            }
        }
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
