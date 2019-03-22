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

@ForwardProvider(AuditActionType.MESSAGE_DELETE)
public class MessageDeleteAuditForwardProvider extends MessageAuditForwardProvider {

    @Override
    protected void build(AuditAction action, MessageBuilder messageBuilder, EmbedBuilder embedBuilder) {
        String messageId = action.getAttribute(MESSAGE_ID, String.class);
        if (action.getChannel() == null || action.getUser() == null || messageId == null) {
            return;
        }

        embedBuilder.setDescription(messageService.getMessage("audit.message.delete.message",
                String.valueOf(action.getGuildId()),
                action.getChannel().getId(),
                messageId));

        embedBuilder.addField(messageService.getMessage("audit.message.deletedContent.title"),
                getMessageValue(action, OLD_CONTENT), false);
        addAuthorField(action, embedBuilder);
        addChannelField(action, embedBuilder);

        embedBuilder.setFooter(messageService.getMessage("audit.message.id", messageId), null);
    }
}
