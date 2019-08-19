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
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.apache.commons.lang3.StringUtils;
import ru.juniperbot.common.persistence.entity.AuditAction;
import ru.juniperbot.common.utils.CommonUtils;

public abstract class ModerationAuditForwardProvider extends LoggingAuditForwardProvider {

    public static final String REASON_ATTR = "reason";

    public static final String GLOBAL_ATTR = "global";

    public static final String DURATION_ATTR = "duration";

    protected void addModeratorField(AuditAction action, EmbedBuilder embedBuilder) {
        if (action.getUser() != null) {
            embedBuilder.addField(messageService.getMessage("audit.moderator.title"),
                    getReferenceContent(action.getUser(), false), true);
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
