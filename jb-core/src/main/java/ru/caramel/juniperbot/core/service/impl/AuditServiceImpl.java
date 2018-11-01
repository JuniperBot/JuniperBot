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
package ru.caramel.juniperbot.core.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.caramel.juniperbot.core.model.AuditActionBuilder;
import ru.caramel.juniperbot.core.model.enums.AuditActionType;
import ru.caramel.juniperbot.core.persistence.entity.AuditAction;
import ru.caramel.juniperbot.core.persistence.entity.AuditConfig;
import ru.caramel.juniperbot.core.persistence.repository.AuditActionRepository;
import ru.caramel.juniperbot.core.persistence.repository.AuditConfigRepository;
import ru.caramel.juniperbot.core.service.AuditService;

@Service
public class AuditServiceImpl
        extends AbstractDomainServiceImpl<AuditConfig, AuditConfigRepository>
        implements AuditService {

    @Autowired
    private AuditActionRepository actionRepository;

    public AuditServiceImpl(@Autowired AuditConfigRepository repository) {
        super(repository, true);
    }

    @Override
    protected AuditConfig createNew(long guildId) {
        AuditConfig config = new AuditConfig(guildId);
        return config;
    }

    private AuditAction save(AuditAction action) {
        // action forwarding notification
        return actionRepository.save(action);
    }

    @Override
    public AuditActionBuilder log(long guildId, AuditActionType type) {
        return new AuditActionBuilder(guildId, type) {
            @Override
            public AuditAction save() {
                return AuditServiceImpl.this.save(this.action);
            }
        };
    }

    @Override
    protected Class<AuditConfig> getDomainClass() {
        return AuditConfig.class;
    }
}
