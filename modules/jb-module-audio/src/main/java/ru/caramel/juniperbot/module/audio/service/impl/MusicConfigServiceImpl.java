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

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.VoiceChannel;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.caramel.juniperbot.core.persistence.entity.GuildConfig;
import ru.caramel.juniperbot.core.service.ConfigService;
import ru.caramel.juniperbot.core.service.DiscordService;
import ru.caramel.juniperbot.core.support.JbCacheManager;
import ru.caramel.juniperbot.module.audio.persistence.entity.MusicConfig;
import ru.caramel.juniperbot.module.audio.persistence.repository.MusicConfigRepository;
import ru.caramel.juniperbot.module.audio.service.MusicConfigService;

@Service
public class MusicConfigServiceImpl implements MusicConfigService {

    @Autowired
    private MusicConfigRepository musicConfigRepository;

    @Autowired
    private DiscordService discordService;

    @Autowired
    private ConfigService configService;

    @Autowired
    private JbCacheManager cacheManager;

    @Override
    @Transactional
    public MusicConfig getConfig(Guild guild) {
        return getConfig(guild.getIdLong());
    }

    @Override
    @Transactional
    public MusicConfig getConfig(long guildId) {
        return cacheManager.get(MusicConfig.class, guildId, e -> {
            MusicConfig config = musicConfigRepository.findByGuildId(e);
            if (config == null) {
                GuildConfig guildConfig = configService.getOrCreate(e);
                config = new MusicConfig();
                config.setGuildConfig(guildConfig);
                config.setVoiceVolume(100);
                musicConfigRepository.save(config);
            }
            return config;
        });
    }

    @Override
    @Transactional
    public void save(MusicConfig config) {
        if (config != null) {
            musicConfigRepository.save(config);
        }
    }

    @Override
    @Transactional
    public boolean hasAccess(Member member) {
        MusicConfig config = getConfig(member.getGuild());
        return config == null
                || CollectionUtils.isEmpty(config.getRoles())
                || member.isOwner()
                || member.hasPermission(Permission.ADMINISTRATOR)
                || member.getRoles().stream().anyMatch(e -> config.getRoles().contains(e.getIdLong()));
    }

    @Override
    @Transactional
    public VoiceChannel getDesiredChannel(Member member) {
        MusicConfig musicConfig = getConfig(member.getGuild());
        VoiceChannel channel = null;
        if (musicConfig != null) {
            if (musicConfig.isUserJoinEnabled() && member.getVoiceState().inVoiceChannel()) {
                channel = member.getVoiceState().getChannel();
            }
            if (channel == null && musicConfig.getChannelId() != null) {
                channel = member.getGuild().getVoiceChannelById(musicConfig.getChannelId());
            }
        }
        if (channel == null) {
            channel = discordService.getDefaultMusicChannel(member.getGuild().getIdLong());
        }
        return channel;
    }

    @Override
    @Transactional
    public void updateVolume(long guildId, int volume) {
        musicConfigRepository.updateVolume(guildId, volume);
        cacheManager.evict(MusicConfig.class, guildId);
    }
}
