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
package ru.juniperbot.common.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.juniperbot.common.configuration.CommonProperties;
import ru.juniperbot.common.persistence.entity.AuditConfig;
import ru.juniperbot.common.persistence.repository.AuditConfigRepository;
import ru.juniperbot.common.service.AuditConfigService;

@Service
public class AuditConfigServiceImpl
        extends AbstractDomainServiceImpl<AuditConfig, AuditConfigRepository>
        implements AuditConfigService {

    public AuditConfigServiceImpl(@Autowired AuditConfigRepository repository,
                                  @Autowired CommonProperties commonProperties) {
        super(repository, commonProperties.getDomainCache().isAuditConfig());
    }

    @Override
    protected AuditConfig createNew(long guildId) {
        return new AuditConfig(guildId);
    }

    @Override
    protected Class<AuditConfig> getDomainClass() {
        return AuditConfig.class;
    }
}
