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
package ru.caramel.juniperbot.ranking.commands;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import ru.caramel.juniperbot.integration.discord.model.DiscordException;
import ru.caramel.juniperbot.model.BotContext;
import ru.caramel.juniperbot.model.DiscordCommand;
import ru.caramel.juniperbot.model.enums.CommandGroup;
import ru.caramel.juniperbot.model.enums.CommandSource;

@DiscordCommand(
        key = "discord.command.leaders.key",
        description = "discord.command.leaders.desc",
        source = CommandSource.GUILD,
        group = CommandGroup.RANKING,
        priority = 201)
public class LeadersCommand extends RankingCommand {

    @Override
    protected boolean doInternal(MessageReceivedEvent message, BotContext context, String content) throws DiscordException {
        message.getTextChannel().sendMessage(messageService.getMessage("discord.command.leaders.message",
                message.getGuild().getId())).submit();
        return true;
    }
}
