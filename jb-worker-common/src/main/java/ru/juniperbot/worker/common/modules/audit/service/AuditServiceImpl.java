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
package ru.juniperbot.worker.common.modules.audit.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.juniperbot.worker.common.modules.audit.model.AuditActionBuilder;
import ru.juniperbot.common.model.AuditActionType;
import ru.juniperbot.worker.common.modules.audit.provider.ForwardProvider;
import ru.juniperbot.common.persistence.entity.AuditAction;
import ru.juniperbot.common.persistence.repository.AuditActionRepository;
import ru.juniperbot.common.persistence.entity.AuditConfig;
import ru.juniperbot.common.service.AuditConfigService;
import ru.juniperbot.worker.common.modules.audit.provider.AuditForwardProvider;
import ru.juniperbot.worker.common.feature.service.FeatureSetService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AuditServiceImpl
        implements AuditService {

    @Value("${discord.audit.durationMonths:1}")
    private int durationMonths;

    @Autowired
    private AuditActionRepository actionRepository;

    @Autowired
    private AuditConfigService configService;

    @Autowired
    private FeatureSetService featureSetService;

    private Map<AuditActionType, AuditForwardProvider> forwardProviders;

    @Override
    @Transactional
    public AuditAction save(AuditAction action) {
        AuditConfig config = configService.getByGuildId(action.getGuildId());
        if (config != null && config.isEnabled()) {
            if (featureSetService.isAvailable(action.getGuildId())) {
                action = actionRepository.save(action);
            }
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
}
