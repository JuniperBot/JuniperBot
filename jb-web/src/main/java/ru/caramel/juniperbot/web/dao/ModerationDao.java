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
package ru.caramel.juniperbot.web.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.caramel.juniperbot.core.moderation.persistence.ModerationConfig;
import ru.caramel.juniperbot.core.moderation.service.ModerationService;
import ru.caramel.juniperbot.web.dto.config.ModerationConfigDto;

@Service
public class ModerationDao extends AbstractDao {

    @Autowired
    private ModerationService moderationService;

    @Transactional
    public ModerationConfigDto getConfig(long guildId) {
        ModerationConfig config = moderationService.getOrCreate(guildId);
        return apiMapper.getModerationDto(config);
    }

    @Transactional
    public void saveConfig(ModerationConfigDto dto, long guildId) {
        ModerationConfig modConfig = moderationService.getOrCreate(guildId);
        apiMapper.updateModerationConfig(dto, modConfig);
        moderationService.save(modConfig);
    }
}
