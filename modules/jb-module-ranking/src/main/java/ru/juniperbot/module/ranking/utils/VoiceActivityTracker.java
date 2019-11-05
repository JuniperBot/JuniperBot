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
package ru.juniperbot.module.ranking.utils;

import com.google.common.util.concurrent.AtomicDouble;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.dv8tion.jda.api.entities.Member;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class VoiceActivityTracker {

    @Getter
    @Setter
    public class MemberState {
        private AtomicLong activityTime = new AtomicLong(0);
        private AtomicDouble points = new AtomicDouble(0);
        private long lastAccumulated = System.currentTimeMillis();
        private boolean frozen;
    }

    private final Map<Long, MemberState> states = new ConcurrentHashMap<>();

    /**
     * Starts voice activity recording for member
     *
     * @param member Member to start record
     */
    public synchronized void add(@NonNull Member member, boolean frozen) {
        accumulate();
        MemberState state = new MemberState();
        state.setFrozen(frozen);
        states.put(getUserId(member), state);
    }

    /**
     * Stops voice activity recording for member
     *
     * @param member Member to start record
     * @return Pair of activity time and points
     */
    public synchronized MemberState remove(@NonNull Member member) {
        accumulate();
        return states.remove(getUserId(member));
    }

    /**
     * (Un-)Freezes member. Frozen members will not gain any activity.
     *
     * @param member Target member
     * @param frozen Whether he is frozen
     */
    public synchronized void freeze(@NonNull Member member, boolean frozen) {
        accumulate();
        MemberState state = states.get(getUserId(member));
        if (state != null) {
            state.setFrozen(frozen);
        }
    }

    public synchronized boolean isEmpty() {
        return states.isEmpty();
    }

    private void accumulate() {
        long now = System.currentTimeMillis();
        long speakingMembers = states.entrySet().stream().filter(e -> !e.getValue().isFrozen()).count();
        states.forEach((userId, state) -> {
            if (!state.isFrozen() && speakingMembers > 1) {
                long duration = now - state.lastAccumulated;
                state.activityTime.addAndGet(duration);
                state.points.addAndGet((duration / 60000.0f) * speakingMembers * 0.4f);
            }
            state.lastAccumulated = now;
        });
    }

    private long getUserId(Member member) {
        return member.getUser().getIdLong();
    }
}
