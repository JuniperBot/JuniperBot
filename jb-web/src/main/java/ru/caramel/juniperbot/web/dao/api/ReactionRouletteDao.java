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
import ru.caramel.juniperbot.module.misc.persistence.entity.ReactionRoulette;
import ru.caramel.juniperbot.module.misc.persistence.repository.ReactionRouletteRepository;
import ru.caramel.juniperbot.web.dao.AbstractDao;
import ru.caramel.juniperbot.web.dto.api.games.ReactionRouletteDto;

@Service
public class ReactionRouletteDao extends AbstractDao {

    @Autowired
    private ReactionRouletteRepository reactionRouletteRepository;

    @Transactional
    public ReactionRouletteDto get(long guildId) {
        ReactionRoulette roulette = reactionRouletteRepository.findByGuildId(guildId);
        if (roulette == null) {
            return new ReactionRouletteDto();
        }
        return apiMapper.getReactionRouletteDto(roulette);
    }

    @Transactional
    public void save(ReactionRouletteDto dto, long guildId) {
        ReactionRoulette roulette = reactionRouletteRepository.findByGuildId(guildId);
        if (roulette == null) {
            roulette = new ReactionRoulette();
            roulette.setGuildConfig(configService.getOrCreate(guildId));
        }
        apiMapper.updateReactionRoulette(dto, roulette);
        reactionRouletteRepository.save(roulette);
    }
}
