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
package ru.caramel.juniperbot.core.model;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import ru.caramel.juniperbot.core.persistence.entity.GuildConfig;
import ru.caramel.juniperbot.core.service.MessageService;

public abstract class AbstractCommand implements Command {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractCommand.class);

    @Autowired
    protected MessageService messageService;

    @Override
    public boolean isAvailable(MessageReceivedEvent event, GuildConfig config) {
        return true;
    }

    protected boolean ok(MessageReceivedEvent message) {
        sendEmotion(message, "✅", null);
        return true;
    }

    protected boolean fail(MessageReceivedEvent message) {
        sendEmotion(message, "❌", null);
        return false;
    }

    protected boolean ok(MessageReceivedEvent message, String messageCode, Object... args) {
        sendEmotion(message, "✅", messageCode, args);
        return true;
    }

    protected boolean fail(MessageReceivedEvent message, String messageCode, Object... args) {
        sendEmotion(message, "❌", messageCode, args);
        return false;
    }

    private void sendEmotion(MessageReceivedEvent message, String emoji, String messageCode, Object... args) {
        try {
            if (message.getGuild() == null || message.getMember().hasPermission(message.getTextChannel(),
                    Permission.MESSAGE_ADD_REACTION)) {
                message.getMessage().addReaction(emoji).submit();
            } else if (StringUtils.isNotEmpty(messageCode)) {
                String text = messageService.getMessage(messageCode, args);
                messageService.sendMessageSilent(message.getChannel()::sendMessage, text);
            }
        } catch (Exception e) {
            LOGGER.error("Add emotion error", e);
        }
    }

    protected Member getMentioned(MessageReceivedEvent event) {
        return event.getGuild() != null && CollectionUtils.isNotEmpty(event.getMessage().getMentionedUsers())
                ? event.getGuild().getMember(event.getMessage().getMentionedUsers().get(0)) : null;
    }
}
