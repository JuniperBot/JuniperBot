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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.caramel.juniperbot.core.persistence.entity.AuditConfig;
import ru.caramel.juniperbot.core.service.AuditService;
import ru.caramel.juniperbot.web.dto.config.AuditConfigDto;

@Service
public class AuditDao extends AbstractDao {

    @Autowired
    private AuditService auditService;

    @Transactional
    public AuditConfigDto get(long guildId) {
        AuditConfig auditConfig = auditService.getByGuildId(guildId);
        return auditConfig != null ? apiMapper.getAuditConfigDto(auditConfig) : new AuditConfigDto();
    }

    @Transactional
    public void save(AuditConfigDto dto, long guildId) {
        AuditConfig auditConfig = auditService.getOrCreate(guildId);
        dto.setForwardChannelId(filterTextChannel(guildId, dto.getForwardChannelId()));
        apiMapper.updateAudit(dto, auditConfig);
        auditService.save(auditConfig);
    }
}
