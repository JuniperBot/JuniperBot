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
package ru.juniperbot.api.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.juniperbot.api.dto.config.MusicConfigDto;
import ru.juniperbot.common.model.discord.GuildDto;
import ru.juniperbot.common.persistence.entity.MusicConfig;
import ru.juniperbot.common.service.MusicConfigService;

@Service
public class MusicDao extends AbstractDao {

    @Autowired
    private MusicConfigService musicConfigService;

    @Transactional
    public MusicConfigDto getConfig(long guildId) {
        MusicConfig musicConfig = musicConfigService.getOrCreate(guildId);
        MusicConfigDto musicConfigDto = apiMapper.getMusicDto(musicConfig);

        GuildDto guildDto = gatewayService.getGuildInfo(guildId);

        if (guildDto != null && (musicConfigDto.getChannelId() == null
                || guildDto.getVoiceChannels().stream().noneMatch(e -> musicConfigDto.getChannelId().equals(e.getId())))) {
            if (guildDto.getDefaultMusicChannelId() != null) {
                musicConfigDto.setChannelId(guildDto.getDefaultMusicChannelId());
            }
        }
        return musicConfigDto;
    }

    @Transactional
    public void saveConfig(MusicConfigDto dto, long guildId) {
        MusicConfig musicConfig = musicConfigService.getOrCreate(guildId);
        dto.setChannelId(filterVoiceChannel(guildId, dto.getChannelId()));
        dto.setTextChannelId(filterTextChannel(guildId, dto.getTextChannelId()));
        apiMapper.updateMusicConfig(dto, musicConfig);
        musicConfigService.save(musicConfig);
    }
}
