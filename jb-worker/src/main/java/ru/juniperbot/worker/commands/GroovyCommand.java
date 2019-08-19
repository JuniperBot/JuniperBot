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
package ru.juniperbot.worker.commands;

import groovy.lang.GroovyShell;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import ru.juniperbot.worker.common.command.model.AbstractCommand;
import ru.juniperbot.worker.common.command.model.BotContext;
import ru.juniperbot.worker.common.command.model.DiscordCommand;
import ru.juniperbot.worker.common.shared.service.DiscordService;
import ru.juniperbot.worker.service.GroovyService;
import ru.juniperbot.common.utils.CommonUtils;

import java.awt.*;

@DiscordCommand(key = "discord.command.groovy.key", description = "discord.command.groovy.desc", priority = 4, hidden = true)
public class GroovyCommand extends AbstractCommand {

    @Autowired
    private DiscordService discordService;

    @Autowired
    private GroovyService groovyService;

    @Override
    public boolean doCommand(GuildMessageReceivedEvent message, BotContext context, String query) {
        if (!discordService.isSuperUser(message.getAuthor()) || StringUtils.isEmpty(query)) {
            return false;
        }
        message.getChannel().sendTyping().queue();
        String script = CommonUtils.unwrapCode(query);
        try {
            Object result = getShell(message).evaluate(script);
            if (result != null) {
                messageService.sendMessageSilent(message.getChannel()::sendMessage,
                        "```groovy\n" + String.valueOf(result) + "```");
            }
        } catch (Exception e) {
            String errorText = String.format("\n`%s`\n\nStack trace:```javascript\n%s", e.getMessage(), ExceptionUtils.getStackTrace(e));
            EmbedBuilder builder = messageService.getBaseEmbed();
            builder.setTitle(e.getClass().getName());
            builder.setColor(Color.RED);
            builder.setDescription(CommonUtils.trimTo(errorText, 2045) + "```");
            messageService.sendMessageSilent(message.getChannel()::sendMessage, builder.build());
            return fail(message);
        }
        return ok(message);
    }

    private GroovyShell getShell(GuildMessageReceivedEvent event) {
        GroovyShell shell = groovyService.createShell();
        shell.setProperty("sm", discordService.getShardManager());
        shell.setProperty("message", event.getMessage());
        shell.setProperty("channel", event.getChannel());
        shell.setProperty("guild", event.getGuild());
        shell.setProperty("member", event.getMember());
        shell.setProperty("author", event.getAuthor());
        return shell;
    }
}
