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

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import ru.caramel.juniperbot.core.service.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractCommand implements Command {

    private static final Pattern MENTION_PATTERN = Pattern.compile("<@!?[0-9]+>\\s*(.*)");

    @Autowired
    protected MessageService messageService;

    @Autowired
    protected ContextService contextService;

    @Autowired
    protected BrandingService brandingService;

    @Autowired
    protected CommandsService commandsService;

    @Autowired
    protected ConfigService configService;

    @Autowired
    protected FeatureSetService featureSetService;

    @Autowired
    protected AuditService auditService;

    @Override
    public boolean isAvailable(User user, Member member, Guild guild) {
        return true;
    }

    protected boolean ok(MessageReceivedEvent message) {
        commandsService.resultEmotion(message, "✅", null);
        return true;
    }

    protected boolean fail(MessageReceivedEvent message) {
        commandsService.resultEmotion(message, "❌", null);
        return false;
    }

    protected boolean ok(MessageReceivedEvent message, String messageCode, Object... args) {
        commandsService.resultEmotion(message, "✅", messageCode, args);
        return true;
    }

    protected boolean fail(MessageReceivedEvent message, String messageCode, Object... args) {
        commandsService.resultEmotion(message, "❌", messageCode, args);
        return false;
    }

    protected Member getMentioned(MessageReceivedEvent event) {
        return event.getGuild() != null && CollectionUtils.isNotEmpty(event.getMessage().getMentionedUsers())
                ? event.getGuild().getMember(event.getMessage().getMentionedUsers().get(0)) : null;
    }

    protected String removeMention(String input) {
        if (StringUtils.isEmpty(input)) {
            return input;
        }
        Matcher matcher = MENTION_PATTERN.matcher(input);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return input;
    }
}
