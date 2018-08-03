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
package ru.caramel.juniperbot.module.misc.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.caramel.juniperbot.core.support.JbCacheManager;
import ru.caramel.juniperbot.module.misc.persistence.entity.ReactionRoulette;
import ru.caramel.juniperbot.module.misc.persistence.repository.ReactionRouletteRepository;
import ru.caramel.juniperbot.module.misc.service.ReactionRouletteService;

@Service
public class ReactionRouletteServiceImpl implements ReactionRouletteService {

    @Autowired
    private ReactionRouletteRepository repository;

    @Autowired
    private JbCacheManager cacheManager;

    @Override
    @Transactional(readOnly = true)
    public ReactionRoulette get(long guildId) {
        return cacheManager.get(ReactionRoulette.class, guildId, repository::findByGuildId);
    }

    @Override
    @Transactional
    public ReactionRoulette save(ReactionRoulette reactionRoulette) {
        return repository.save(reactionRoulette);
    }
}
