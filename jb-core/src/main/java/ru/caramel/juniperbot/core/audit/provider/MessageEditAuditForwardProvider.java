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

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import ru.caramel.juniperbot.core.audit.model.AuditActionType;
import ru.caramel.juniperbot.core.audit.model.ForwardProvider;
import ru.caramel.juniperbot.core.audit.persistence.AuditAction;

@ForwardProvider(AuditActionType.MESSAGE_EDIT)
public class MessageEditAuditForwardProvider extends MessageAuditForwardProvider {

    public static final String NEW_CONTENT = "new";

    @Override
    protected void build(AuditAction action, MessageBuilder messageBuilder, EmbedBuilder embedBuilder) {
        String messageId = action.getAttribute(MESSAGE_ID, String.class);
        if (action.getChannel() == null || action.getUser() == null || messageId == null) {
            return;
        }

        embedBuilder.setDescription(messageService.getMessage("audit.message.edit.message",
                String.valueOf(action.getGuildId()),
                action.getChannel().getId(),
                messageId));

        addOldContentField(action, embedBuilder);
        embedBuilder.addField(messageService.getMessage("audit.message.newContent.title"),
                getMessageValue(action, NEW_CONTENT), false);

        addAuthorField(action, embedBuilder);
        addChannelField(action, embedBuilder);

        embedBuilder.setFooter(messageService.getMessage("audit.message.id", messageId), null);
    }
}
