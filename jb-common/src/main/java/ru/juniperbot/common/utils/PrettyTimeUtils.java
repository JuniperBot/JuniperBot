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
package ru.juniperbot.common.utils;

import org.ocpsoft.prettytime.Duration;
import org.ocpsoft.prettytime.PrettyTime;
import org.ocpsoft.prettytime.TimeUnit;
import org.ocpsoft.prettytime.units.JustNow;
import org.ocpsoft.prettytime.units.Millisecond;

import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PrettyTimeUtils {

    private PrettyTimeUtils() {
        // private
    }

    public static String formatDuration(Date date, Locale locale) {
        PrettyTime formatter = new PrettyTime(new Date(0), locale);
        formatter.removeUnit(JustNow.class);
        formatter.removeUnit(Millisecond.class);
        List<Duration> durations = formatter.calculatePreciseDuration(date);
        if (durations.isEmpty()) {
            return null;
        }
        if (Locale.US.equals(locale)) {
            return formatter.format(durations);
        }
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < durations.size(); i++) {
            Duration duration = durations.get(i);
            if (i == 0) {
                result.append(formatter.format(duration));
            } else {
                result.append(formatter.format(decorateDuration(duration)));
            }
            result.append(" ");
        }
        return result.toString().trim();
    }

    private static Duration decorateDuration(Duration duration) {
        return new Duration() {
            @Override
            public long getQuantity() {
                return duration.getQuantity();
            }

            @Override
            public long getQuantityRounded(int i) {
                return duration.getQuantityRounded(i);
            }

            @Override
            public TimeUnit getUnit() {
                return duration.getUnit();
            }

            @Override
            public long getDelta() {
                return duration.getDelta();
            }

            @Override
            public boolean isInPast() {
                return false;
            }

            @Override
            public boolean isInFuture() {
                return false;
            }
        };
    }
}
