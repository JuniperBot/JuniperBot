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
package ru.juniperbot.module.ranking.utils

import com.google.common.cache.CacheBuilder
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.VoiceChannel
import ru.juniperbot.common.service.RankingConfigService
import ru.juniperbot.module.ranking.model.MemberVoiceState

import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

class GuildVoiceActivityTracker(private val configService: RankingConfigService,
                                private val guildId: Long) {

    private val trackers = CacheBuilder.newBuilder()
            .concurrencyLevel(4)
            .expireAfterAccess(10, TimeUnit.DAYS)
            .build<Long, VoiceActivityTracker>()/* Channel Id */

    val isEmpty: Boolean
        @Synchronized get() = trackers.size() == 0L

    /**
     * Starts voice activity recording for member
     *
     * @param channel Target voice channel
     * @param member  Member to start record
     * @param frozen  Whether is he frozen
     */
    @Synchronized
    fun add(channel: VoiceChannel, member: Member, frozen: Boolean) {
        try {
            val tracker = trackers.get(channel.idLong) { VoiceActivityTracker(configService, guildId) }
            tracker.add(member, frozen)
        } catch (e: ExecutionException) {
            // ignore
        }
    }

    /**
     * Stops voice activity recording for member
     *
     * @param channel Target voice channel
     * @param member  Member to start record
     * @return Pair of activity time and points
     */
    @Synchronized
    fun remove(channel: VoiceChannel, member: Member): MemberVoiceState? {
        val tracker = trackers.getIfPresent(channel.idLong) ?: return null
        val state = tracker.remove(member)
        if (tracker.isEmpty) {
            trackers.invalidate(channel.idLong)
        }
        return state
    }

    /**
     * (Un-)Freezes member. Frozen members will not gain activity points.
     *
     * @param member Target member
     * @param frozen Whether is he frozen
     */
    @Synchronized
    fun freeze(member: Member, frozen: Boolean) {
        trackers.asMap().forEach { (_, tracker) -> tracker.freeze(member, frozen) }
    }
}
