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

import net.dv8tion.jda.api.entities.Member
import ru.juniperbot.common.service.RankingConfigService
import ru.juniperbot.module.ranking.model.MemberVoiceState
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min

class VoiceActivityTracker(private val configService: RankingConfigService,
                           private val guildId: Long = 0) {

    private val states = ConcurrentHashMap<Long, MemberVoiceState>()

    val isEmpty: Boolean
        @Synchronized get() = states.isEmpty()

    /**
     * Starts voice activity recording for member
     *
     * @param member Member to start record
     */
    @Synchronized
    fun add(member: Member, frozen: Boolean) {
        accumulate()
        states[member.user.idLong] = MemberVoiceState().apply { this.frozen = frozen }
    }

    /**
     * Stops voice activity recording for member
     *
     * @param member Member to start record
     * @return Pair of activity time and points
     */
    @Synchronized
    fun remove(member: Member): MemberVoiceState? {
        accumulate()
        return states.remove(member.user.idLong)
    }

    /**
     * (Un-)Freezes member. Frozen members will not gain any activity.
     *
     * @param member Target member
     * @param frozen Whether he is frozen
     */
    @Synchronized
    fun freeze(member: Member, frozen: Boolean) {
        accumulate()
        val state = states[member.user.idLong]
        if (state != null) {
            state.frozen = frozen
        }
    }

    private fun accumulate() {
        val now = System.currentTimeMillis()
        var speakingMembers = states.entries.filter { !it.value.frozen }.count()
        val maxVoiceMembers = configService.getMaxVoiceMembers(guildId)
        if (maxVoiceMembers != null) {
            speakingMembers = min(speakingMembers, maxVoiceMembers)
        }
        states.forEach { (_, state) ->
            if (!state.frozen && speakingMembers > 1) {
                val duration = now - state.lastAccumulated
                state.activityTime.addAndGet(duration)
                state.points.addAndGet((duration / 60000.0f * speakingMembers.toFloat() * 0.4f).toDouble())
            }
            state.lastAccumulated = now
        }
    }
}
