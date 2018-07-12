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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.caramel.juniperbot.core.persistence.entity.GuildConfig;
import ru.caramel.juniperbot.core.persistence.repository.GuildConfigRepository;
import ru.caramel.juniperbot.module.moderation.persistence.entity.ModerationConfig;
import ru.caramel.juniperbot.module.moderation.service.ModerationService;
import ru.caramel.juniperbot.web.dao.AbstractDao;
import ru.caramel.juniperbot.web.dto.api.config.CommonConfigDto;
import ru.caramel.juniperbot.web.dto.api.config.ModerationConfigDto;

@Service
public class CommonDao extends AbstractDao {

    @Autowired
    private GuildConfigRepository repository;

    @Autowired
    private ModerationService moderationService;

    @Transactional
    public CommonConfigDto getConfig(long guildId) {
        GuildConfig config = configService.getOrCreate(guildId);
        CommonConfigDto dto = apiMapper.getCommonDto(config);

        ModerationConfig modConfig = moderationService.getConfig(guildId);
        ModerationConfigDto modConfigDto = apiMapper.getModerationDto(modConfig);
        dto.setModConfig(modConfigDto);
        return dto;
    }

    @Transactional
    public void saveConfig(CommonConfigDto dto, long guildId) {
        GuildConfig config = configService.getOrCreate(guildId);
        apiMapper.updateCommon(dto, config);

        ModerationConfigDto modConfigDto = dto.getModConfig();
        ModerationConfig modConfig = moderationService.getConfig(guildId);
        apiMapper.updateModerationConfig(modConfigDto, modConfig);
        moderationService.save(modConfig);

        repository.save(config);
    }
}
