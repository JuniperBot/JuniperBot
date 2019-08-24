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
package ru.juniperbot.module.misc.commands;

import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import ru.juniperbot.common.worker.command.model.AbstractCommand;
import ru.juniperbot.common.worker.command.model.BotContext;
import ru.juniperbot.common.worker.command.model.DiscordCommand;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@DiscordCommand(key = "discord.command.rand.key",
        description = "discord.command.rand.desc",
        group = "discord.command.group.utility",
        priority = 19)
public class RandCommand extends AbstractCommand {

    private static final Pattern RANGE_PATTERN = Pattern.compile("^(\\d+)\\s+(\\d+)$");

    @Override
    public boolean doCommand(GuildMessageReceivedEvent message, BotContext context, String query) {
        try {
            if (StringUtils.isNumeric(query)) {
                return showResult(message, 0, Long.parseLong(query));
            }

            Matcher matcher = RANGE_PATTERN.matcher(query);
            if (matcher.find()) {
                long min = Long.parseLong(matcher.group(1));
                long max = Long.parseLong(matcher.group(2));
                if (min >= max) {
                    messageService.onError(message.getChannel(), "discord.command.rand.wrongRange");
                    return false;
                }
                return showResult(message, min, max);
            }
        } catch (NumberFormatException e) {
            messageService.onError(message.getChannel(), "discord.command.rand.wrongNumber");
            return false;
        }

        String commandKey = messageService.getMessageByLocale("discord.command.rand.key", context.getCommandLocale());
        messageService.onEmbedMessage(message.getChannel(), "discord.command.rand.help",
                context.getConfig().getPrefix(), commandKey);
        return true;
    }

    private boolean showResult(GuildMessageReceivedEvent message, long min, long max) {
        message.getChannel().sendMessage(String.valueOf(RandomUtils.nextLong(min, max + 1))).queue();
        return true;
    }
}
