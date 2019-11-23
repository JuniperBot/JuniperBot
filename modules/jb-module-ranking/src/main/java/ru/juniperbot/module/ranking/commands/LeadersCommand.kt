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
package ru.juniperbot.module.ranking.commands

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import org.springframework.beans.factory.annotation.Autowired
import ru.juniperbot.common.service.RankingConfigService
import ru.juniperbot.common.worker.command.model.AbstractCommand
import ru.juniperbot.common.worker.command.model.BotContext
import ru.juniperbot.common.worker.command.model.DiscordCommand

@DiscordCommand(
        key = "discord.command.leaders.key",
        description = "discord.command.leaders.desc",
        group = ["discord.command.group.ranking"],
        priority = 201)
class LeadersCommand : AbstractCommand() {

    @Autowired
    lateinit var rankingConfigService: RankingConfigService

    override fun doCommand(message: GuildMessageReceivedEvent, context: BotContext, content: String): Boolean {
        val leaders = messageService.getMessage("discord.command.leaders.message", message.guild.id);
        message.channel.sendMessage(leaders).queue()
        return true
    }

    override fun isAvailable(user: User, member: Member, guild: Guild): Boolean {
        return rankingConfigService.isEnabled(guild.idLong)
    }
}
