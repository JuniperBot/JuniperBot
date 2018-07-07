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
package ru.caramel.juniperbot.web.dao.api;

import net.dv8tion.jda.core.entities.VoiceChannel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.caramel.juniperbot.module.audio.persistence.entity.MusicConfig;
import ru.caramel.juniperbot.module.audio.service.MusicConfigService;
import ru.caramel.juniperbot.web.dao.AbstractDao;
import ru.caramel.juniperbot.web.dto.api.config.MusicConfigDto;

@Service
public class MusicDao extends AbstractDao {

    @Autowired
    private MusicConfigService musicConfigService;

    @Transactional
    public MusicConfigDto getConfig(long guildId) {
        MusicConfig musicConfig = musicConfigService.getConfig(guildId);
        MusicConfigDto musicConfigDto = apiMapper.getMusicDto(musicConfig);
        if (discordService.isConnected(guildId) && (musicConfigDto.getChannelId() == null ||
                discordService.getShardManager().getVoiceChannelById(musicConfigDto.getChannelId()) == null)) {
            VoiceChannel channel = discordService.getDefaultMusicChannel(guildId);
            if (channel != null) {
                musicConfigDto.setChannelId(channel.getId());
            }
        }
        return musicConfigDto;
    }

    @Transactional
    public void saveConfig(MusicConfigDto dto, long guildId) {
        MusicConfig musicConfig = musicConfigService.getConfig(guildId);
        apiMapper.updateMusicConfig(dto, musicConfig);
        musicConfigService.save(musicConfig);
    }
}
