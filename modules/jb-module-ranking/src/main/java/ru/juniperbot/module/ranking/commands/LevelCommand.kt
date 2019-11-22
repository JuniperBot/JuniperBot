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

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import ru.juniperbot.common.model.request.RankingUpdateRequest
import ru.juniperbot.common.service.RankingConfigService
import ru.juniperbot.common.utils.RankingUtils
import ru.juniperbot.common.worker.command.model.BotContext
import ru.juniperbot.common.worker.command.model.DiscordCommand
import ru.juniperbot.common.worker.command.model.MemberReference
import ru.juniperbot.common.worker.command.model.MentionableCommand
import ru.juniperbot.common.worker.modules.moderation.service.ModerationService
import ru.juniperbot.module.ranking.service.RankingService

@DiscordCommand(
        key = "discord.command.level.key",
        description = "discord.command.level.desc",
        group = ["discord.command.group.ranking"],
        permissions = [Permission.MESSAGE_WRITE, Permission.MESSAGE_EMBED_LINKS],
        priority = 245)
class LevelCommand protected constructor() : MentionableCommand(false, true) {

    @Autowired
    lateinit var moderationService: ModerationService

    @Autowired
    lateinit var rankingConfigService: RankingConfigService

    @Autowired
    lateinit var rankingService: RankingService

    override fun doCommand(reference: MemberReference, event: GuildMessageReceivedEvent, context: BotContext, content: String): Boolean {
        if (reference.localMember == null) {
            showHelp(event, context)
            return false
        }
        if (!StringUtils.isNumeric(content)) {
            showHelp(event, context)
            return false
        }

        val level: Int
        try {
            level = Integer.parseInt(content)
        } catch (e: NumberFormatException) {
            showHelp(event, context)
            return false
        }

        if (level < 0 || level > RankingUtils.MAX_LEVEL) {
            showHelp(event, context)
            return false
        }
        val request = RankingUpdateRequest(reference.localMember).apply { this.level = level }
        contextService.queue(event.guild, event.channel.sendTyping()) { _ ->
            rankingConfigService.update(request)
            if (reference.member != null) {
                rankingService.updateRewards(reference.member)
            }
            messageService.onTempEmbedMessage(event.channel, 10, "discord.command.level.success",
                    "**${reference.member?.asMention ?: reference.localMember.effectiveName}**", level)
        }
        return true
    }

    override fun showHelp(event: GuildMessageReceivedEvent, context: BotContext) {
        val command = messageService.getMessageByLocale("discord.command.level.key", context.commandLocale)
        messageService.onEmbedMessage(event.channel, "discord.command.level.help",
                RankingUtils.MAX_LEVEL, context.config.prefix, command)
    }

    override fun isAvailable(user: User, member: Member, guild: Guild): Boolean {
        return rankingConfigService.isEnabled(guild.idLong) && moderationService.isModerator(member)
    }
}
