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
package ru.juniperbot.worker.common.event.listeners;

import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.core.task.TaskExecutor;
import ru.juniperbot.worker.common.audit.service.AuditService;
import ru.juniperbot.worker.common.event.service.ContextService;

public abstract class DiscordEventListener extends ListenerAdapter {

    @Autowired
    @Qualifier("executor")
    protected TaskExecutor taskExecutor;

    @Autowired
    protected ContextService contextService;

    @Autowired
    protected ApplicationContext applicationContext;

    private AuditService auditService;

    protected AuditService getAuditService() {
        if (auditService == null) {
            auditService = applicationContext.getBean(AuditService.class);
        }
        return auditService;
    }
}
