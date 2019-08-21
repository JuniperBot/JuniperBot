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
package ru.juniperbot.module.ranking.commands;

import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import ru.juniperbot.common.worker.command.model.BotContext;
import ru.juniperbot.common.worker.command.model.DiscordCommand;

@DiscordCommand(
        key = "discord.command.leaders.key",
        description = "discord.command.leaders.desc",
        group = "discord.command.group.ranking",
        priority = 201)
public class LeadersCommand extends RankingCommand {

    @Override
    protected boolean doInternal(GuildMessageReceivedEvent message, BotContext context, String content) {
        message.getChannel().sendMessage(messageService.getMessage("discord.command.leaders.message",
                message.getGuild().getId())).queue();
        return true;
    }
}
