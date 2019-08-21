/*
 * This file is part of JuniperBot.
 *
 * JuniperBot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * JuniperBot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with JuniperBot. If not, see <http://www.gnu.org/licenses/>.
 */
package ru.juniperbot.api.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.juniperbot.api.dto.config.RankingDto;
import ru.juniperbot.api.service.ApiMapperService;
import ru.juniperbot.common.persistence.entity.RankingConfig;
import ru.juniperbot.common.service.RankingConfigService;
import ru.juniperbot.common.utils.RankingUtils;

import java.util.stream.Collectors;

@Service
public class RankingDao extends AbstractDao {

    @Autowired
    private RankingConfigService rankingService;

    @Autowired
    private MessageTemplateDao templateDao;

    @Transactional
    public RankingDto get(long guildId) {
        RankingConfig config = rankingService.getOrCreate(guildId);
        return apiMapper.getRankingDto(config);
    }

    @Transactional
    public void save(RankingDto dto, long guildId) {
        RankingConfig config = rankingService.getOrCreate(guildId);
        config.setAnnouncementEnabled(dto.isAnnouncementEnabled());
        config.setEnabled(dto.isEnabled());
        config.setResetOnLeave(dto.isResetOnLeave());
        config.setBannedRoles(dto.getBannedRoles());
        config.setIgnoredChannels(ApiMapperService.toLongList(dto.getIgnoredChannels()));
        config.setCookieEnabled(dto.isCookieEnabled());

        config.setAnnounceTemplate(templateDao.updateOrCreate(dto.getAnnounceTemplate(), config.getAnnounceTemplate()));

        if (dto.getRewards() != null) {
            config.setRewards(dto.getRewards().stream()
                    .filter(e -> e.getLevel() != null && e.getLevel() >= 0 && e.getLevel() <= RankingUtils.MAX_LEVEL)
                    .collect(Collectors.toList()));
        } else {
            config.setRewards(null);
        }
        rankingService.save(config);
    }
}
