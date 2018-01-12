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

import net.dv8tion.jda.core.entities.Guild;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.caramel.juniperbot.module.ranking.persistence.entity.RankingConfig;
import ru.caramel.juniperbot.module.ranking.service.RankingService;
import ru.caramel.juniperbot.module.ranking.utils.RankingUtils;
import ru.caramel.juniperbot.web.dto.RankingConfigDto;

import java.util.stream.Collectors;

@Service
public class RankingDao extends AbstractDao {

    @Autowired
    private RankingService rankingService;

    @Transactional
    public RankingConfigDto getConfig(long serverId) {
        return mapper.getRankingDto(rankingService.getConfig(serverId));
    }

    @Transactional
    public void saveConfig(RankingConfigDto configDto, long serverId) {
        RankingConfig config = rankingService.getConfig(serverId);
        config.setAnnouncementEnabled(configDto.isAnnouncementEnabled());
        config.setEnabled(configDto.isEnabled());
        config.setWhisper(configDto.isWhisper());
        config.setAnnouncement(configDto.getAnnouncement());
        config.setResetOnLeave(configDto.isResetOnLeave());
        if (discordService.isConnected()) {
            config.setBannedRoles(configDto.getBannedRoles());
            if (configDto.getRewards() != null) {
                config.setRewards(configDto.getRewards().stream()
                        .filter(e -> e.getLevel() != null && e.getLevel() >= 0 && e.getLevel() <= RankingUtils.MAX_LEVEL)
                        .collect(Collectors.toList()));
            } else {
                config.setRewards(null);
            }

        }
        rankingService.save(config);
        if (discordService.isConnected()) {
            Guild guild = discordService.getJda().getGuildById(serverId);
            if (guild != null) {
                rankingService.sync(guild);
            }
        }
    }
}
