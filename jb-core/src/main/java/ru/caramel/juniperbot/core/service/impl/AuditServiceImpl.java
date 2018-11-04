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

import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.caramel.juniperbot.core.audit.AuditForwardProvider;
import ru.caramel.juniperbot.core.model.AuditActionBuilder;
import ru.caramel.juniperbot.core.model.ForwardProvider;
import ru.caramel.juniperbot.core.model.enums.AuditActionType;
import ru.caramel.juniperbot.core.persistence.entity.AuditAction;
import ru.caramel.juniperbot.core.persistence.entity.AuditConfig;
import ru.caramel.juniperbot.core.persistence.repository.AuditActionRepository;
import ru.caramel.juniperbot.core.persistence.repository.AuditConfigRepository;
import ru.caramel.juniperbot.core.service.AuditService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AuditServiceImpl
        extends AbstractDomainServiceImpl<AuditConfig, AuditConfigRepository>
        implements AuditService {

    @Autowired
    private AuditActionRepository actionRepository;

    private Map<AuditActionType, AuditForwardProvider> forwardProviders;

    public AuditServiceImpl(@Autowired AuditConfigRepository repository) {
        super(repository, true);
    }

    @Override
    protected AuditConfig createNew(long guildId) {
        AuditConfig config = new AuditConfig(guildId);
        return config;
    }

    @Override
    @Transactional
    public AuditAction save(AuditAction action) {
        AuditConfig config = getByGuildId(action.getGuildId());
        if (config != null && config.isEnabled()) {
            action = actionRepository.save(action);
            if (MapUtils.isNotEmpty(forwardProviders)) {
                AuditForwardProvider forwardProvider = forwardProviders.get(action.getActionType());
                if (forwardProvider != null) {
                    forwardProvider.send(config, action);
                }
            }
        }
        return action;
    }

    @Override
    public AuditActionBuilder log(long guildId, AuditActionType type) {
        return new AuditActionBuilder(guildId, type) {
            @Override
            @Transactional
            public AuditAction save() {
                return AuditServiceImpl.this.save(this.action);
            }
        };
    }

    @Autowired(required = false)
    private void setForwardProviders(List<AuditForwardProvider> forwardProviders) {
        this.forwardProviders = forwardProviders.stream().collect(Collectors.toMap(
                e -> e.getClass().isAnnotationPresent(ForwardProvider.class)
                        ? e.getClass().getAnnotation(ForwardProvider.class).value() : null, e -> e));
    }

    @Override
    protected Class<AuditConfig> getDomainClass() {
        return AuditConfig.class;
    }
}
