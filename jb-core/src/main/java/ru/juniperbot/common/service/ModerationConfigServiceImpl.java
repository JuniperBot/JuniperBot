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
package ru.juniperbot.common.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.juniperbot.common.persistence.entity.ModerationConfig;
import ru.juniperbot.common.persistence.repository.ModerationConfigRepository;

import java.util.ArrayList;

@Service
public class ModerationConfigServiceImpl
        extends AbstractDomainServiceImpl<ModerationConfig, ModerationConfigRepository>
        implements ModerationConfigService {

    public ModerationConfigServiceImpl(@Autowired ModerationConfigRepository repository) {
        super(repository, true);
    }

    @Override
    protected ModerationConfig createNew(long guildId) {
        ModerationConfig config = new ModerationConfig(guildId);
        config.setCoolDownIgnored(true);
        config.setActions(new ArrayList<>());
        return config;
    }

    @Override
    protected Class<ModerationConfig> getDomainClass() {
        return ModerationConfig.class;
    }
}
