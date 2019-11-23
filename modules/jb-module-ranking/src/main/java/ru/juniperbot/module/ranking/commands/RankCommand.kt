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

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import org.springframework.beans.factory.annotation.Autowired
import ru.juniperbot.common.model.RankingInfo
import ru.juniperbot.common.persistence.entity.RankingConfig
import ru.juniperbot.common.service.RankingConfigService
import ru.juniperbot.common.utils.CommonUtils
import ru.juniperbot.common.worker.command.model.BotContext
import ru.juniperbot.common.worker.command.model.DiscordCommand
import ru.juniperbot.common.worker.command.model.MemberReference
import ru.juniperbot.common.worker.command.model.MentionableCommand
import ru.juniperbot.module.ranking.service.RankingService
import ru.juniperbot.module.render.model.ReportType
import ru.juniperbot.module.render.service.ImagingService
import ru.juniperbot.module.render.service.JasperReportsService
import ru.juniperbot.module.render.utils.ImageUtils
import java.awt.Font
import java.awt.RenderingHints

@DiscordCommand(
        key = "discord.command.rank.key",
        description = "discord.command.rank.desc",
        group = ["discord.command.group.ranking"],
        priority = 202)
class RankCommand protected constructor() : MentionableCommand(true, true) {

    @Autowired
    lateinit var rankingConfigService: RankingConfigService

    @Autowired
    lateinit var rankingService: RankingService

    @Autowired
    lateinit var reportsService: JasperReportsService

    @Autowired
    lateinit var imagingService: ImagingService

    override fun doCommand(reference: MemberReference,
                           event: GuildMessageReceivedEvent,
                           context: BotContext,
                           content: String): Boolean {
        contextService.queue(event.guild, event.channel.sendTyping()) queue@{ _ ->
            val member = reference.localMember
            val info = rankingService.getRankingInfo(event.guild.idLong, reference.id)!!
            val config = rankingConfigService.get(event.guild)

            val self = event.guild.selfMember
            if (self.hasPermission(event.channel, Permission.MESSAGE_ATTACH_FILES)
                    && sendCard(event.channel, reference, config, info)) {
                return@queue
            }

            val builder = messageService.getBaseEmbed(true)
            addFields(builder, config, info, event.guild)

            val desiredPage = info.rank / 50 + 1
            val url = "https://juniper.bot/ranking/${event.guild.id}?page=${desiredPage}#${reference.id}"
            builder.setAuthor(member.effectiveName, url, member.user.avatarUrl)
            messageService.sendMessageSilent({ event.channel.sendMessage(it) }, builder.build())
        }
        return true
    }

    override fun isAvailable(user: User, member: Member, guild: Guild): Boolean {
        return rankingConfigService.isEnabled(guild.idLong)
    }

    private fun sendCard(channel: TextChannel, reference: MemberReference, config: RankingConfig?, info: RankingInfo): Boolean {
        val templateMap = mutableMapOf<String, Any>()
        templateMap["name"] = "" // it fails on font fallback so we have to render it on our own
        templateMap["avatarImage"] = if (reference.member != null)
            imagingService.getAvatarWithStatus(reference.member)
        else
            imagingService.getAvatarWithStatus(reference.localMember)
        templateMap["backgroundImage"] = imagingService.getResourceImage("ranking-card-background.png")
        templateMap["percent"] = info.pct
        templateMap["remainingExp"] = info.remainingExp
        templateMap["levelExp"] = info.levelExp
        templateMap["totalExp"] = info.totalExp
        templateMap["level"] = info.level
        templateMap["rank"] = info.rank
        if (config != null && config.isCookieEnabled) {
            templateMap["cookies"] = info.cookies
        }
        if (info.voiceActivity > 0) {
            templateMap["voiceActivity"] = CommonUtils.formatDuration(info.voiceActivity)
        }
        templateMap["rankText"] = messageService.getMessage("discord.command.rank.info.rank.short.title")
        templateMap["levelText"] = messageService.getMessage("discord.command.rank.info.lvl.short.title")

        val cardImage = reportsService.generateImage(ReportType.RANKING_CARD, templateMap) ?: return false

        cardImage.createGraphics().apply {
            val fontVariants = reportsService.loadFontVariants(Font.PLAIN, 40f, "Roboto Light")
            this.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            this.drawString(ImageUtils.createFallbackString(reference.effectiveName, fontVariants).iterator, 200, 63)
            this.dispose()
        }

        val cardBytes = ImageUtils.getImageBytes(cardImage, "png") ?: return false
        channel.sendFile(cardBytes, "ranking-card.png").queue()
        return true
    }

    fun addFields(builder: EmbedBuilder, config: RankingConfig, info: RankingInfo, guild: Guild) {
        val totalMembers = rankingConfigService.countRankings(guild.idLong)
        builder.addField(messageService.getMessage("discord.command.rank.info.rank.title"),
                String.format("# %d/%d", info.rank, totalMembers), true)
        builder.addField(messageService.getMessage("discord.command.rank.info.lvl.title"),
                info.level.toString(), true)
        builder.addField(messageService.getMessage("discord.command.rank.info.exp.title"),
                messageService.getMessage("discord.command.rank.info.exp.format",
                        info.remainingExp, info.levelExp, info.totalExp), true)
        if (config.isCookieEnabled) {
            builder.addField(messageService.getMessage("discord.command.rank.info.cookies.title"),
                    String.format("%d \uD83C\uDF6A", info.cookies), true)
        }
        if (info.voiceActivity > 0) {
            builder.addField(messageService.getMessage("discord.command.rank.info.voiceActivity.title"),
                    CommonUtils.formatDuration(info.voiceActivity), true)
        }
    }
}
