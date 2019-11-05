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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.VoiceChannel;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Slf4j
public class GuildVoiceActivityTracker {

    private Cache</* Channel Id */Long, VoiceActivityTracker> trackers = CacheBuilder.newBuilder()
            .concurrencyLevel(4)
            .expireAfterAccess(10, TimeUnit.DAYS)
            .build();

    /**
     * Starts voice activity recording for member
     *
     * @param channel Target voice channel
     * @param member  Member to start record
     * @param frozen  Whether is he frozen
     */
    public synchronized void add(@NonNull VoiceChannel channel, @NonNull Member member, boolean frozen) {
        try {
            VoiceActivityTracker tracker = trackers.get(channel.getIdLong(), VoiceActivityTracker::new);
            tracker.add(member, frozen);
        } catch (ExecutionException e) {
            log.warn("Can't start record for channel [{}] with member [{}]", channel, member, e);
        }
    }

    /**
     * Stops voice activity recording for member
     *
     * @param channel Target voice channel
     * @param member  Member to start record
     * @return Pair of activity time and points
     */
    public synchronized VoiceActivityTracker.MemberState remove(@NonNull VoiceChannel channel, @NonNull Member member) {
        VoiceActivityTracker tracker = trackers.getIfPresent(channel.getIdLong());
        if (tracker == null) {
            return null;
        }
        VoiceActivityTracker.MemberState state = tracker.remove(member);
        if (tracker.isEmpty()) {
            trackers.invalidate(channel.getIdLong());
        }
        return state;
    }

    /**
     * (Un-)Freezes member. Frozen members will not gain activity points.
     *
     * @param member Target member
     * @param frozen Whether is he frozen
     */
    public synchronized void freeze(@NonNull Member member, boolean frozen) {
        trackers.asMap().forEach((channelId, tracker) -> tracker.freeze(member, frozen));
    }

    public synchronized boolean isEmpty() {
        return trackers.size() == 0;
    }
}
