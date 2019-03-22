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
package ru.caramel.juniperbot.core.metrics.model;

import com.codahale.metrics.*;
import org.apache.commons.collections4.MapUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class TimeWindowChart implements Reservoir, PersistentMetric, Gauge<Long> {

    private final ConcurrentSkipListMap<Long, Long> measurements;
    private long window;
    private final AtomicLong lastTick;
    private volatile Long lastMeasurement;

    public TimeWindowChart() {
        this(10, TimeUnit.MINUTES);
    }

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
    public synchronized int size() {
        trim();
        return measurements.size();
    }

    @Override
    public synchronized void update(long value) {
        trim();
        measurements.put(getTime(), value);
    }

    @Override
    public synchronized Snapshot getSnapshot() {
        trim();
        return new UniformSnapshot(measurements.values());
    }

    public synchronized Map<Long, Long> getMeasurements() {
        trim();
        return Collections.unmodifiableMap(measurements);
    }

    private synchronized long getTime() {
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

    @Override
    public synchronized Map<String, Object> toMap() {
        Map<String, Object> objectMap = new HashMap<>();
        objectMap.put("window", window);
        objectMap.put("measurements", new HashMap<>(measurements));
        return objectMap;
    }

    @Override
    @SuppressWarnings("unchecked")
    public synchronized void fromMap(Map<String, Object> data) {
        if (MapUtils.isEmpty(data)) {
            return;
        }
        Object window = data.get("window");
        Object measurements = data.get("measurements");
        if (window instanceof Number && measurements instanceof Map) {
            this.window = ((Number) window).longValue();
            this.measurements.clear();
            ((Map) measurements).forEach((k, v) ->
                    this.measurements.put(Long.parseLong(k.toString()), Long.parseLong(v.toString())));
            this.lastTick.set(System.currentTimeMillis());
            trim();
        }
    }

    @Override
    public Long getValue() {
        return lastMeasurement;
    }
}
