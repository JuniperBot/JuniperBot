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
package ru.juniperbot.worker.common.audit.provider;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import ru.juniperbot.common.model.AuditActionType;
import ru.juniperbot.common.persistence.entity.AuditAction;

@ForwardProvider(AuditActionType.MEMBER_NAME_CHANGE)
public class NicknameChangeAuditForwardProvider extends LoggingAuditForwardProvider {

    public static final String OLD_NAME = "old";

    public static final String NEW_NAME = "new";

    @Override
    protected void build(AuditAction action, MessageBuilder messageBuilder, EmbedBuilder embedBuilder) {
        if (action.getUser() != null) {
            embedBuilder.setDescription(messageService.getMessage("audit.message.nickname.change.message",
                    getReferenceContent(action.getUser(), false)));

            embedBuilder.addField(messageService.getMessage("audit.message.nickname.old.title"),
                    action.getAttribute(OLD_NAME, String.class), true);
            embedBuilder.addField(messageService.getMessage("audit.message.nickname.new.title"),
                    action.getAttribute(NEW_NAME, String.class), true);

            embedBuilder.setFooter(messageService.getMessage("audit.member.id", action.getUser().getId()), null);
        }
    }
}
