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
package ru.juniperbot.module.ranking.service

import com.google.common.cache.CacheBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.scheduling.TaskScheduler
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.juniperbot.common.configuration.CommonConfiguration
import ru.juniperbot.common.extensions.modifyMemberRolesDelayed
import ru.juniperbot.common.model.RankingInfo
import ru.juniperbot.common.model.RankingReward
import ru.juniperbot.common.persistence.entity.Cookie
import ru.juniperbot.common.persistence.entity.LocalMember
import ru.juniperbot.common.persistence.entity.Ranking
import ru.juniperbot.common.persistence.entity.RankingConfig
import ru.juniperbot.common.persistence.repository.CookieRepository
import ru.juniperbot.common.persistence.repository.RankingRepository
import ru.juniperbot.common.service.MemberService
import ru.juniperbot.common.service.RankingConfigService
import ru.juniperbot.common.service.TransactionHandler
import ru.juniperbot.common.utils.RankingUtils
import ru.juniperbot.common.worker.feature.service.FeatureSetService
import ru.juniperbot.common.worker.message.service.MessageTemplateService
import ru.juniperbot.common.worker.shared.service.DiscordEntityAccessor
import ru.juniperbot.common.worker.shared.service.DiscordService
import ru.juniperbot.module.ranking.model.GainExpResult
import ru.juniperbot.module.ranking.model.MemberVoiceState
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.roundToLong
import kotlin.random.Random

@Service
class RankingServiceImpl : RankingService {

    @Autowired
    lateinit var rankingRepository: RankingRepository

    @Autowired
    lateinit var cookieRepository: CookieRepository

    @Autowired
    lateinit var memberService: MemberService

    @Autowired
    lateinit var discordService: DiscordService

    @Autowired
    lateinit var templateService: MessageTemplateService

    @Autowired
    lateinit var entityAccessor: DiscordEntityAccessor

    @Autowired
    lateinit var configService: RankingConfigService

    @Autowired
    lateinit var featureSetService: FeatureSetService

    @Autowired
    lateinit var transactionHandler: TransactionHandler

    @Autowired
    @Qualifier(CommonConfiguration.SCHEDULER)
    lateinit var scheduler: TaskScheduler

    private val coolDowns = CacheBuilder.newBuilder()
            .concurrencyLevel(4)
            .expireAfterWrite(60, TimeUnit.SECONDS)
            .build<String, Boolean>()

    private val cookieCoolDown: Date
        get() = DateTime.now().minusMinutes(10).toDate()

    @Transactional
    override fun onMessage(event: GuildMessageReceivedEvent) {
        val config = configService.get(event.guild)
        if (config == null
                || event.member == null
                || !config.isEnabled
                || !memberService.isApplicable(event.member)
                || configService.isBanned(config, event.member)) {
            return
        }

        val memberKey = "${event.guild.id}_${event.author.id}"
        val gainedExp = if (coolDowns.getIfPresent(memberKey) == null && !isIgnoredChannel(config, event.channel))
            (Random.nextLong(15, 25) * config.textExpMultiplier).roundToLong() else 0

        val hasCookie = config.isCookieEnabled
                && event.message.mentionedUsers.isNotEmpty()
                && event.message.contentRaw.contains(RankingService.COOKIE_EMOTE)

        val cookieRecipients = if (!hasCookie) emptyList() else
            event.message.mentionedMembers.filter { !it.user.isBot && it.user != event.author && memberService.isApplicable(it) }

        if (gainedExp == 0L && cookieRecipients.isEmpty()) {
            return
        }

        updateRanking(config, event.member!!, gainedExp, event.channel) preProcessor@{ ranking ->
            if (cookieRecipients.isEmpty()) {
                return@preProcessor
            }
            val checkDate = cookieCoolDown
            cookieRecipients.forEach { giveCookie(ranking.member, entityAccessor.getOrCreate(it), checkDate) }
        }
        if (gainedExp > 0) {
            coolDowns.put(memberKey, true)
        }
    }

    @Transactional
    override fun giveCookie(senderMember: Member, recipientMember: Member) {
        if (!memberService.isApplicable(senderMember) || !memberService.isApplicable(recipientMember)) {
            return
        }
        val config = configService.get(senderMember.guild)
        if (config != null && config.isCookieEnabled) {
            transactionHandler.runWithLockRetry {
                val recipient = entityAccessor.getOrCreate(recipientMember)
                val sender = entityAccessor.getOrCreate(senderMember)
                giveCookie(sender, recipient, cookieCoolDown)
            }
        }
    }

    @Transactional
    override fun addVoiceActivity(member: Member, state: MemberVoiceState) {
        if (!memberService.isApplicable(member)) {
            return
        }
        val config = configService.get(member.guild)
        if (config == null || !config.isEnabled) {
            return
        }
        val gainedExp = if (config.isVoiceEnabled
                && featureSetService.isAvailable(member.guild)
                && !configService.isBanned(config, member))
            (15.0 * state.points.get() * config.voiceExpMultiplier).roundToLong() else 0
        // we should delay reward roles  (cuz they may conflict with InVoice roles)
        updateRanking(config, member, gainedExp, null, 3000) {
            it.voiceActivity += state.activityTime.get()
        }
    }

    private fun updateRanking(config: RankingConfig,
                              member: Member,
                              gainedExp: Long,
                              notifyChannel: TextChannel? = null,
                              roleDelay: Long? = null,
                              preProcess: (Ranking) -> Unit) {
        val (result, oldLevel, newLevel) = transactionHandler.runWithLockRetry {
            val ranking = getRanking(member)
            preProcess.invoke(ranking)
            val oldLevel = RankingUtils.getLevelFromExp(ranking.exp)
            ranking.exp += gainedExp
            val newLevel = RankingUtils.getLevelFromExp(ranking.exp)
            rankingRepository.save(ranking)
            GainExpResult(ranking, oldLevel, newLevel)
        }
        if (oldLevel < newLevel) {
            if (config.isAnnouncementEnabled) {
                // it is lazy and out of current session
                val template = if (config.announceTemplate == null) null else
                    templateService.getById(config.announceTemplate.id)
                templateService
                        .createMessage(template)
                        .withFallbackContent("discord.command.rank.levelup")
                        .withGuild(member.guild)
                        .withMember(member)
                        .withFallbackChannel(notifyChannel)
                        .withDirectAllowed(true)
                        .withVariable("level", newLevel)
                        .compileAndSend()
            }
            updateRewards(config, member, result, roleDelay)
        }
    }

    private fun giveCookie(sender: LocalMember, recipient: LocalMember, checkDate: Date) {
        if (cookieRepository.isFull(sender, recipient, checkDate)) {
            return
        }
        cookieRepository.save(Cookie(sender, recipient))
        val recipientRanking = configService.getRanking(recipient)
        if (recipientRanking != null) {
            recipientRanking.incrementCookies()
            rankingRepository.save(recipientRanking)
        }
    }

    @Transactional
    override fun updateRewards(member: Member) {
        val config = configService.get(member.guild)
        if (config != null) {
            val ranking = getRanking(member)
            updateRewards(config, member, ranking)
        }
    }

    private fun updateRewards(config: RankingConfig, member: Member, ranking: Ranking, delay: Long? = null) {
        val self = member.guild.selfMember
        if (!discordService.isConnected(member.guild.idLong)
                || config.rewards.isNullOrEmpty()
                || !self.hasPermission(Permission.MANAGE_ROLES)) {
            return
        }

        val newLevel = RankingUtils.getLevelFromExp(ranking.exp)

        var rewards = config.rewards
                .filter { it.roleId != null && it.level <= newLevel }
                .sortedBy { it.level }

        if (rewards.isEmpty()) {
            return
        }
        val highest = rewards.last()
        rewards = rewards.dropLast(1)

        val rewardsToAdd = mutableListOf<RankingReward>()
        val rewardsToRemove = mutableListOf<RankingReward>()
        rewards.forEach { (if (it.isReset) rewardsToRemove else rewardsToAdd).add(it) }
        rewardsToAdd.add(highest)

        val rolesToAdd = getRoles(member, rewardsToAdd)
        val rolesToRemove = getRoles(member, rewardsToRemove)
        member.guild.modifyMemberRolesDelayed(scheduler, member, rolesToAdd, rolesToRemove, delay)
    }

    private fun getRoles(member: Member, rewards: List<RankingReward>): List<Role> {
        val self = member.guild.selfMember
        return rewards
                .mapNotNull { member.guild.getRoleById(it.roleId) }
                .filter { self.canInteract(it) && !it.isManaged }
    }

    @Transactional
    override fun getRankingInfo(member: Member): RankingInfo {
        val ranking = getRanking(member)
        val rankingInfo = RankingUtils.calculateInfo(ranking)
        rankingInfo.rank = rankingRepository.getRank(member.guild.idLong, rankingInfo.totalExp)
        return rankingInfo
    }

    private fun getRanking(member: Member): Ranking {
        var ranking: Ranking? = configService.getRanking(member)
        if (ranking == null) {
            val localMember = entityAccessor.getOrCreate(member)
            ranking = Ranking()
            ranking.member = localMember
            rankingRepository.save(ranking)
        }
        return ranking
    }

    @Transactional
    override fun getRankingInfo(member: LocalMember): RankingInfo {
        val ranking = getRanking(member)
        val rankingInfo = RankingUtils.calculateInfo(ranking)
        rankingInfo.rank = rankingRepository.getRank(member.guildId, rankingInfo.totalExp)
        return rankingInfo
    }

    private fun getRanking(member: LocalMember): Ranking {
        var ranking = configService.getRanking(member)
        if (ranking == null) {
            ranking = Ranking()
            ranking.member = memberService.get(member.guildId, member.user.userId) // force attach to transaction
            rankingRepository.save(ranking)
        }
        return ranking
    }

    private fun isIgnoredChannel(config: RankingConfig, channel: TextChannel): Boolean {
        return config.ignoredChannels != null && config.ignoredChannels.contains(channel.idLong)
    }
}
