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
package ru.caramel.juniperbot.web.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.caramel.juniperbot.core.audit.model.AuditActionType;
import ru.caramel.juniperbot.core.audit.persistence.AuditAction;
import ru.caramel.juniperbot.core.audit.persistence.AuditActionRepository;
import ru.caramel.juniperbot.core.audit.persistence.AuditAction_;
import ru.caramel.juniperbot.core.audit.persistence.AuditConfig;
import ru.caramel.juniperbot.core.audit.service.AuditService;
import ru.caramel.juniperbot.core.common.persistence.base.NamedReference_;
import ru.caramel.juniperbot.web.dto.AuditActionDto;
import ru.caramel.juniperbot.web.dto.config.AuditConfigDto;
import ru.caramel.juniperbot.web.dto.request.AuditActionRequest;

import java.util.Date;
import java.util.List;

@Service
public class AuditDao extends AbstractDao {

    @Autowired
    private AuditService auditService;

    @Autowired
    private AuditActionRepository actionRepository;

    @Transactional
    public AuditConfigDto get(long guildId) {
        AuditConfig auditConfig = auditService.getByGuildId(guildId);
        return auditConfig != null ? apiMapper.getAuditConfigDto(auditConfig) : new AuditConfigDto();
    }

    @Transactional
    public List<AuditActionDto> getActions(long guildId, AuditActionRequest request) {
        Specification<AuditAction> spec = rootAuditSpec(guildId);
        if (request != null) {
            if (request.getActionType() != null) {
                spec = spec.and(withActionType(request.getActionType()));
            }
            if (request.getUserId() != null) {
                spec = spec.and(withUserId(request.getUserId()));
            }
            if (request.getChannelId() != null) {
                spec = spec.and(withChannelId(request.getChannelId()));
            }
            if (request.getOlderThan() != null) {
                spec = spec.and(withOlderThan(request.getOlderThan()));
            }
            if (request.getStartDate() != null) {
                spec = spec.and(withStartDate(request.getStartDate()));
            }
            if (request.getEndDate() != null) {
                spec = spec.and(withEndDate(request.getEndDate()));
            }
        }
        List<AuditAction> actions = actionRepository.findAll(spec, PageRequest.of(0, 50)).getContent();
        return apiMapper.getAuditActionDtos(actions);
    }

    @Transactional
    public void save(AuditConfigDto dto, long guildId) {
        AuditConfig auditConfig = auditService.getOrCreate(guildId);
        dto.setForwardChannelId(filterTextChannel(guildId, dto.getForwardChannelId()));
        apiMapper.updateAudit(dto, auditConfig);
        auditService.save(auditConfig);
    }

    private static Specification<AuditAction> rootAuditSpec(long withGuildId) {
        return (root, query, builder) ->  {
            query.orderBy(builder.desc(root.get(AuditAction_.actionDate)));
            return builder.equal(root.get(AuditAction_.guildId), withGuildId);
        };
    }

    private static Specification<AuditAction> withActionType(AuditActionType actionType) {
        return (root, query, builder) -> builder.equal(root.get(AuditAction_.actionType), actionType);
    }

    private static Specification<AuditAction> withUserId(String userId) {
        return (root, query, builder) -> builder.or(
                builder.equal(root.get(AuditAction_.user).get(NamedReference_.id), userId),
                builder.equal(root.get(AuditAction_.user).get(NamedReference_.id), userId)
        );
    }

    private static Specification<AuditAction> withOlderThan(Date olderThan) {
        return (root, query, builder) -> builder.lessThan(root.get(AuditAction_.actionDate), olderThan);
    }

    private static Specification<AuditAction> withStartDate(Date date) {
        return (root, query, builder) -> builder.greaterThanOrEqualTo(root.get(AuditAction_.actionDate), date);
    }

    private static Specification<AuditAction> withEndDate(Date date) {
        return (root, query, builder) -> builder.lessThanOrEqualTo(root.get(AuditAction_.actionDate), date);
    }

    private static Specification<AuditAction> withChannelId(String userId) {
        return (root, query, builder) -> builder.equal(root.get(AuditAction_.channel).get(NamedReference_.id), userId);
    }
}
