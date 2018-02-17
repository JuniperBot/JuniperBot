/*
 * This file is part of JuniperBotJ.
 *
 * JuniperBotJ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * JuniperBotJ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with JuniperBotJ. If not, see <http://www.gnu.org/licenses/>.
 */
package ru.caramel.juniperbot.core.model;

import com.codahale.metrics.*;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class TimeWindowChart implements Reservoir {

    private final ConcurrentSkipListMap<Long, Long> measurements;
    private final long window;
    private final AtomicLong lastTick;

    /**
     * Creates a new {@link SlidingTimeWindowReservoir} with the given window of time.
     *
     * @param window     the window of time
     * @param windowUnit the unit of {@code window}
     */
    public TimeWindowChart(long window, TimeUnit windowUnit) {
        this.measurements = new ConcurrentSkipListMap<>();
        this.window = windowUnit.toMillis(window);
        this.lastTick = new AtomicLong(System.currentTimeMillis());
    }

    @Override
    public int size() {
        trim();
        return measurements.size();
    }

    @Override
    public void update(long value) {
        trim();
        measurements.put(getTime(), value);
    }

    @Override
    public Snapshot getSnapshot() {
        trim();
        return new UniformSnapshot(measurements.values());
    }

    public Map<Long, Long> getMeasurements() {
        trim();
        return Collections.unmodifiableMap(measurements);
    }

    private long getTime() {
        for (; ; ) {
            final long oldTick = lastTick.get();
            final long tick = System.currentTimeMillis();
            // ensure the tick is strictly incrementing even if there are duplicate ticks
            final long newTick = tick - oldTick > 0 ? tick : oldTick + 1;
            if (lastTick.compareAndSet(oldTick, newTick)) {
                return newTick;
            }
        }
    }

    private void trim() {
        final long windowEnd = getTime();
        final long windowStart = windowEnd - window;
        if (windowStart < windowEnd) {
            measurements.headMap(windowStart).clear();
            measurements.tailMap(windowEnd).clear();
        } else {
            measurements.subMap(windowEnd, windowStart).clear();
        }
    }
}
