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
package ru.juniperbot.module.ranking.listeners

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.guild.member.GuildMemberLeaveEvent
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent
import org.springframework.beans.factory.annotation.Autowired
import ru.juniperbot.common.model.request.RankingUpdateRequest
import ru.juniperbot.common.service.RankingConfigService
import ru.juniperbot.common.worker.event.DiscordEvent
import ru.juniperbot.common.worker.event.listeners.DiscordEventListener
import ru.juniperbot.module.ranking.service.RankingService

@DiscordEvent
class RankingListener : DiscordEventListener() {

    @Autowired
    lateinit var rankingService: RankingService

    @Autowired
    lateinit var rankingConfigService: RankingConfigService

    override fun onGuildMemberLeave(event: GuildMemberLeaveEvent) {
        taskExecutor.execute {
            val config = rankingConfigService.getByGuildId(event.guild.idLong)
            if (config != null && config.isResetOnLeave) {
                rankingConfigService.update(RankingUpdateRequest(event.guild.idLong,
                        event.member.user.id,
                        0,
                        true,
                        true))
            }
        }
    }

    override fun onGuildMessageReactionAdd(event: GuildMessageReactionAddEvent) {
        val sender = event.member
        if (sender.user.isBot || event.reactionEmote.name != RankingService.COOKIE_EMOTE) {
            return
        }
        val self = event.guild.selfMember
        val channel = event.channel
        if (!self.hasPermission(channel, Permission.MESSAGE_HISTORY)) {
            return
        }
        channel.retrieveMessageById(event.messageId).queue { m ->
            if (!m.author.isBot && m.author != sender.user) {
                val recipient = event.guild.getMember(m.author)
                if (recipient != null) {
                    rankingService.giveCookie(sender, recipient)
                }
            }
        }
    }
}
