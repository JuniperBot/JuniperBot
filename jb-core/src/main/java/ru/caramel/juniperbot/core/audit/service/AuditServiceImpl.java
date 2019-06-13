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
package ru.caramel.juniperbot.core.audit.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.caramel.juniperbot.core.audit.model.AuditActionBuilder;
import ru.caramel.juniperbot.core.audit.model.AuditActionType;
import ru.caramel.juniperbot.core.audit.model.ForwardProvider;
import ru.caramel.juniperbot.core.audit.persistence.AuditAction;
import ru.caramel.juniperbot.core.audit.persistence.AuditActionRepository;
import ru.caramel.juniperbot.core.audit.persistence.AuditConfig;
import ru.caramel.juniperbot.core.audit.persistence.AuditConfigRepository;
import ru.caramel.juniperbot.core.audit.provider.AuditForwardProvider;
import ru.caramel.juniperbot.core.common.service.AbstractDomainServiceImpl;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AuditServiceImpl
        extends AbstractDomainServiceImpl<AuditConfig, AuditConfigRepository>
        implements AuditService {

    @Value("${discord.audit.durationMonths:6}")
    private int durationMonths;

    @Autowired
    private AuditActionRepository actionRepository;

    private Map<AuditActionType, AuditForwardProvider> forwardProviders;

    public AuditServiceImpl(@Autowired AuditConfigRepository repository) {
        super(repository, true);
    }

    @Override
    protected AuditConfig createNew(long guildId) {
        return new AuditConfig(guildId);
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

    @Scheduled(cron = "0 0 0 1 * ?")
    @Transactional
    public void runCleanUp() {
        runCleanUp(this.durationMonths);
    }

    @Override
    @Transactional
    public void runCleanUp(int durationMonths) {
        log.info("Starting audit cleanup for {} months old", durationMonths);
        actionRepository.deleteByActionDateBefore(DateTime.now().minusMonths(durationMonths).toDate());
        log.info("Audit cleanup finished");
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
