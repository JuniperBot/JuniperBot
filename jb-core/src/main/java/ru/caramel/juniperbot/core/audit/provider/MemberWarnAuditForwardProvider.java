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
package ru.caramel.juniperbot.core.audit.provider;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import ru.caramel.juniperbot.core.audit.model.AuditActionType;
import ru.caramel.juniperbot.core.audit.model.ForwardProvider;
import ru.caramel.juniperbot.core.audit.persistence.AuditAction;

@ForwardProvider(AuditActionType.MEMBER_WARN)
public class MemberWarnAuditForwardProvider extends ModerationAuditForwardProvider {

    public static final String COUNT_ATTR = "count";

    public static final String MAX_ATTR = "max";

    @Override
    protected void build(AuditAction action, MessageBuilder messageBuilder, EmbedBuilder embedBuilder) {
        if (action.getTargetUser() == null) {
            return;
        }
        String message = messageService.getMessage("audit.member.warn.message",
                getReferenceContent(action.getTargetUser(), false));
        embedBuilder.setDescription(message);

        addModeratorField(action, embedBuilder);

        Number count = action.getAttribute(COUNT_ATTR, Number.class);
        Number max = action.getAttribute(MAX_ATTR, Number.class);
        if (count != null && max != null) {
            embedBuilder.addField(messageService.getMessage("audit.member.warn.count.title"),
                    messageService.getMessage("audit.member.warn.count.content", count, max), true);
        }

        addReasonField(action, embedBuilder);

        embedBuilder.setFooter(messageService.getMessage("audit.member.id", action.getTargetUser().getId()), null);
    }
}
