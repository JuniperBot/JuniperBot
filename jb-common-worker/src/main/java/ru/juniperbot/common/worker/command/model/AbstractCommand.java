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
package ru.juniperbot.common.worker.command.model;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import ru.juniperbot.common.configuration.CommonProperties;
import ru.juniperbot.common.service.ConfigService;
import ru.juniperbot.common.worker.command.service.InternalCommandsService;
import ru.juniperbot.common.worker.event.service.ContextService;
import ru.juniperbot.common.worker.feature.service.FeatureSetService;
import ru.juniperbot.common.worker.message.service.MessageService;
import ru.juniperbot.common.worker.shared.service.DiscordEntityAccessor;
import ru.juniperbot.common.worker.shared.service.DiscordService;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractCommand implements Command {

    private static final Pattern MENTION_PATTERN = Pattern.compile("<@!?[0-9]+>\\s*(.*)");

    @Autowired
    protected DiscordService discordService;

    @Autowired
    protected MessageService messageService;

    @Autowired
    protected ContextService contextService;

    @Autowired
    protected CommonProperties commonProperties;

    @Autowired
    protected InternalCommandsService commandsService;

    @Autowired
    protected ConfigService configService;

    @Autowired
    protected FeatureSetService featureSetService;

    @Autowired
    protected ApplicationContext applicationContext;

    @Autowired
    protected DiscordEntityAccessor entityAccessor;

    private DiscordCommand annotation;

    @Override
    public boolean isAvailable(User user, Member member, Guild guild) {
        return true;
    }

    protected boolean ok(GuildMessageReceivedEvent message) {
        commandsService.resultEmotion(message, "✅", null);
        return true;
    }

    protected boolean fail(GuildMessageReceivedEvent message) {
        commandsService.resultEmotion(message, "❌", null);
        return false;
    }

    protected boolean ok(GuildMessageReceivedEvent message, String messageCode, Object... args) {
        commandsService.resultEmotion(message, "✅", messageCode, args);
        return true;
    }

    protected boolean fail(GuildMessageReceivedEvent message, String messageCode, Object... args) {
        commandsService.resultEmotion(message, "❌", messageCode, args);
        return false;
    }

    protected Member getMentioned(GuildMessageReceivedEvent event) {
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
        String[] lines = input.split("\\r?\\n");
        boolean foundFirst = false;
        for (int i = 0; i < lines.length; i++) {
            Matcher matcher = MENTION_PATTERN.matcher(lines[i]);
            if (!foundFirst && matcher.find()) {
                foundFirst = true;
                lines[i] = matcher.group(1).trim();
            }
        }
        return StringUtils.join(lines, "\n").trim();
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
