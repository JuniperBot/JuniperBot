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
package ru.caramel.juniperbot.module.welcome.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.caramel.juniperbot.core.support.JbCacheManager;
import ru.caramel.juniperbot.module.welcome.persistence.entity.WelcomeMessage;
import ru.caramel.juniperbot.module.welcome.persistence.repository.WelcomeMessageRepository;
import ru.caramel.juniperbot.module.welcome.service.WelcomeService;

@Service
public class WelcomeServiceImpl implements WelcomeService {

    @Autowired
    private WelcomeMessageRepository repository;

    @Autowired
    private JbCacheManager cacheManager;

    @Override
    @Transactional(readOnly = true)
    public WelcomeMessage get(long guildId) {
        return cacheManager.get(WelcomeMessage.class, guildId, repository::findByGuildId);
    }

    @Override
    @Transactional
    public WelcomeMessage save(WelcomeMessage reactionRoulette) {
        return repository.save(reactionRoulette);
    }
}
