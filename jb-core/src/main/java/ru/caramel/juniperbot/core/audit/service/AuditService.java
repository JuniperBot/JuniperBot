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

import net.dv8tion.jda.core.entities.Guild;
import ru.caramel.juniperbot.core.audit.model.AuditActionBuilder;
import ru.caramel.juniperbot.core.audit.model.AuditActionType;
import ru.caramel.juniperbot.core.audit.persistence.AuditAction;
import ru.caramel.juniperbot.core.audit.persistence.AuditConfig;
import ru.caramel.juniperbot.core.common.service.DomainService;

public interface AuditService extends DomainService<AuditConfig> {

    AuditAction save(AuditAction action);

    AuditActionBuilder log(long guildId, AuditActionType type);

    default AuditActionBuilder log(Guild guild, AuditActionType type) {
        return log(guild.getIdLong(), type);
    }
}
