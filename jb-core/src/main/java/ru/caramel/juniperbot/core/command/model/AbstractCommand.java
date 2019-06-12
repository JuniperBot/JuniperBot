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
package ru.caramel.juniperbot.core.command.model;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import ru.caramel.juniperbot.core.audit.service.AuditService;
import ru.caramel.juniperbot.core.command.service.CommandsService;
import ru.caramel.juniperbot.core.common.service.BrandingService;
import ru.caramel.juniperbot.core.common.service.ConfigService;
import ru.caramel.juniperbot.core.event.service.ContextService;
import ru.caramel.juniperbot.core.feature.service.FeatureSetService;
import ru.caramel.juniperbot.core.message.service.MessageService;

import java.util.List;
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
    protected ApplicationContext applicationContext;

    private AuditService auditService;

    private DiscordCommand annotation;

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
        if (event.getGuild() == null || CollectionUtils.isEmpty(event.getMessage().getMentionedMembers())) {
            return null;
        }
        List<Member> mentioned = event.getMessage().getMentionedMembers();
        return mentioned.get(mentioned.size() - 1);
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

    protected AuditService getAuditService() {
        if (auditService == null) {
            auditService = applicationContext.getBean(AuditService.class);
        }
        return auditService;
    }

    @Override
    public DiscordCommand getAnnotation() {
        if (annotation == null) {
            synchronized (this) {
                if (annotation == null) {
                    annotation = getClass().getDeclaredAnnotation(DiscordCommand.class);
                }
            }
        }
        return annotation;
    }
}
