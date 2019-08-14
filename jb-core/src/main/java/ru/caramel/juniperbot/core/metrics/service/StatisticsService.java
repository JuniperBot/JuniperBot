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
package ru.caramel.juniperbot.core.metrics.service;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import net.dv8tion.jda.api.JDA;
import ru.caramel.juniperbot.core.metrics.model.TimeWindowChart;

import java.util.concurrent.TimeUnit;

public interface StatisticsService {

    Timer getTimer(String name);

    Meter getMeter(String name);

    Counter getCounter(String name);

    TimeWindowChart getTimeChart(String name, long window, TimeUnit windowUnit);

    void notifyProviders(JDA shard);

    void persistMetrics();

    boolean isDetailed();

    void setDetailed(boolean detailed);

    void doWithTimer(String name, Runnable action);

    void doWithTimer(Timer timer, Runnable action);
}
