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
package ru.juniperbot.common.worker.modules.audit.provider;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import ru.juniperbot.common.model.AuditActionType;
import ru.juniperbot.common.persistence.entity.AuditAction;

@ForwardProvider(AuditActionType.MEMBER_LEAVE)
public class MemberLeaveAuditForwardProvider extends LoggingAuditForwardProvider {

    @Override
    protected void build(AuditAction action, MessageBuilder messageBuilder, EmbedBuilder embedBuilder) {
        if (action.getUser() == null) {
            return;
        }
        String message = messageService.getMessage("audit.member.leave.message",
                getReferenceContent(action.getUser(), false));
        embedBuilder.setDescription(message);
        embedBuilder.setFooter(messageService.getMessage("audit.member.id", action.getUser().getId()), null);
    }
}
