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

@ForwardProvider(AuditActionType.MESSAGES_CLEAR)
public class MessagesClearAuditForwardProvider extends ModerationAuditForwardProvider {

    public static final String MESSAGE_COUNT = "message_count";

    @Override
    protected void build(AuditAction action, MessageBuilder messageBuilder, EmbedBuilder embedBuilder) {
        Integer messageCount = action.getAttribute(MESSAGE_COUNT, Integer.class);
        if (action.getChannel() == null || action.getUser() == null || messageCount == null) {
            return;
        }
        String pluralMessages = messageService.getCountPlural(messageCount, "discord.plurals.message");
        embedBuilder.setDescription(messageService.getMessage("audit.message.delete.messages", messageCount, pluralMessages));
        addModeratorField(action, embedBuilder);
        addChannelField(action, embedBuilder);
    }
}
