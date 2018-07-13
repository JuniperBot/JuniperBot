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

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.caramel.juniperbot.module.ranking.persistence.entity.RankingConfig;
import ru.caramel.juniperbot.module.ranking.service.RankingService;
import ru.caramel.juniperbot.module.ranking.utils.RankingUtils;
import ru.caramel.juniperbot.web.dao.AbstractDao;
import ru.caramel.juniperbot.web.dto.api.config.RankingDto;

import java.util.stream.Collectors;

@Service
public class RankingDao extends AbstractDao {

    private final static String WHISPER_CHANNEL = "-1";

    private final static String MESSAGE_CHANNEL = "-2";

    @Autowired
    private RankingService rankingService;

    @Transactional
    public RankingDto get(long guildId) {
        RankingConfig config = rankingService.getConfig(guildId);
        RankingDto dto = apiMapper.getRankingDto(config);
        if (config.isWhisper()) {
            dto.setAnnouncementChannelId(WHISPER_CHANNEL);
        } else if (dto.getAnnouncementChannelId() == null) {
            dto.setAnnouncementChannelId(MESSAGE_CHANNEL);
        }
        return dto;
    }

    @Transactional
    public void save(RankingDto dto, long guildId) {
        RankingConfig config = rankingService.getConfig(guildId);
        config.setAnnouncementEnabled(dto.isAnnouncementEnabled());
        config.setEnabled(dto.isEnabled());
        config.setAnnouncement(dto.getAnnouncement());
        config.setResetOnLeave(dto.isResetOnLeave());
        config.setEmbed(dto.isEmbed());
        config.setBannedRoles(dto.getBannedRoles());

        config.setWhisper(WHISPER_CHANNEL.equals(dto.getAnnouncementChannelId()));
        if (config.isWhisper()) {
            config.setAnnouncementChannelId(null);
        } else {
            config.setAnnouncementChannelId(StringUtils.isNumeric(dto.getAnnouncementChannelId())
                    ? Long.parseLong(dto.getAnnouncementChannelId()) : null);
        }

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
