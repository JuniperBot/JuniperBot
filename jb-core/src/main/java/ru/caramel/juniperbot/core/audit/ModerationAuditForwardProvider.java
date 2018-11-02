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
package ru.caramel.juniperbot.core.audit;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import org.apache.commons.lang3.StringUtils;
import ru.caramel.juniperbot.core.persistence.entity.AuditAction;
import ru.caramel.juniperbot.core.utils.CommonUtils;

public abstract class ModerationAuditForwardProvider extends LoggingAuditForwardProvider {

    public static final String REASON_ATTR = "reason";

    protected void addModeratorField(AuditAction action, EmbedBuilder embedBuilder) {
        if (action.getUser() != null) {
            embedBuilder.addField(messageService.getMessage("audit.moderator.title"),
                    messageService.getMessage("audit.moderator.content",
                            action.getUser().getName(),
                            action.getUser().getAsUserMention()), true);
        }
    }

    protected void addReasonField(AuditAction action, EmbedBuilder embedBuilder) {
        String reason = action.getAttribute(REASON_ATTR, String.class);
        if (StringUtils.isNotEmpty(reason)) {
            embedBuilder.addField(messageService.getMessage("audit.reason"),
                    CommonUtils.trimTo(reason, MessageEmbed.TEXT_MAX_LENGTH), true);
        }
    }
}
