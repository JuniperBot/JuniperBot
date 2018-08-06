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
import ru.caramel.juniperbot.core.service.DiscordService;
import ru.caramel.juniperbot.core.service.impl.AbstractDomainServiceImpl;
import ru.caramel.juniperbot.core.support.JbCacheManager;
import ru.caramel.juniperbot.module.audio.persistence.entity.MusicConfig;
import ru.caramel.juniperbot.module.audio.persistence.repository.MusicConfigRepository;
import ru.caramel.juniperbot.module.audio.service.MusicConfigService;

@Service
public class MusicConfigServiceImpl extends AbstractDomainServiceImpl<MusicConfig, MusicConfigRepository> implements MusicConfigService {

    @Autowired
    private DiscordService discordService;

    @Autowired
    private JbCacheManager cacheManager;

    public MusicConfigServiceImpl(@Autowired MusicConfigRepository repository) {
        super(repository);
    }

    @Override
    @Transactional
    public MusicConfig getOrCreate(Guild guild) {
        return getOrCreate(guild.getIdLong()); // to make it cacheable
    }

    @Override
    @Transactional
    public MusicConfig getOrCreate(long guildId) {
        return cacheManager.get(MusicConfig.class, guildId, super::getOrCreate);
    }

    @Override
    protected MusicConfig createNew(long guildId) {
        MusicConfig config = new MusicConfig(guildId);
        config.setVoiceVolume(100);
        return config;
    }

    @Override
    @Transactional
    public boolean hasAccess(Member member) {
        MusicConfig config = getOrCreate(member.getGuild());
        return config == null
                || CollectionUtils.isEmpty(config.getRoles())
                || member.isOwner()
                || member.hasPermission(Permission.ADMINISTRATOR)
                || member.getRoles().stream().anyMatch(e -> config.getRoles().contains(e.getIdLong()));
    }

    @Override
    @Transactional
    public VoiceChannel getDesiredChannel(Member member) {
        MusicConfig musicConfig = getOrCreate(member.getGuild());
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
        repository.updateVolume(guildId, volume);
        cacheManager.evict(MusicConfig.class, guildId);
    }
}
