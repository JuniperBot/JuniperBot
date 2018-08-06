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

import net.dv8tion.jda.core.entities.Guild;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.caramel.juniperbot.core.service.impl.AbstractDomainServiceImpl;
import ru.caramel.juniperbot.core.support.JbCacheManager;
import ru.caramel.juniperbot.module.misc.persistence.entity.ReactionRoulette;
import ru.caramel.juniperbot.module.misc.persistence.repository.ReactionRouletteRepository;
import ru.caramel.juniperbot.module.misc.service.ReactionRouletteService;

@Service
public class ReactionRouletteServiceImpl extends AbstractDomainServiceImpl<ReactionRoulette, ReactionRouletteRepository> implements ReactionRouletteService {

    public ReactionRouletteServiceImpl(@Autowired ReactionRouletteRepository repository) {
        super(repository);
    }

    @Autowired
    private JbCacheManager cacheManager;

    @Override
    @Transactional(readOnly = true)
    public ReactionRoulette getByGuildId(long guildId) {
        return cacheManager.get(ReactionRoulette.class, guildId, super::getByGuildId);
    }

    @Override
    @Transactional
    public ReactionRoulette getOrCreate(Guild guild) {
        return getOrCreate(guild.getIdLong()); // to make it cacheable
    }

    @Override
    @Transactional
    public ReactionRoulette getOrCreate(long guildId) {
        return cacheManager.get(ReactionRoulette.class, guildId, super::getOrCreate);
    }

    @Override
    protected ReactionRoulette createNew(long guildId) {
        return new ReactionRoulette(guildId);
    }
}
