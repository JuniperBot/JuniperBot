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
package ru.caramel.juniperbot.core.support.jmx;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

import java.util.concurrent.ScheduledThreadPoolExecutor;

@Slf4j
@ManagedResource
public class JmxJDAMBean implements JmxNamedResource {

    private final JDA jda;

    private final ScheduledThreadPoolExecutor rateLimitPool;

    public JmxJDAMBean(JDA jda) {
        this.jda = jda;
        this.rateLimitPool = (ScheduledThreadPoolExecutor) jda.getRateLimitPool();
    }

    /* =====================================================
                             COMMON
       ===================================================== */

    @ManagedAttribute(description = "Returns the ping of shard")
    public long getGatewayPing() {
        return jda.getGatewayPing();
    }

    @ManagedAttribute(description = "Returns the status of shard")
    public String getStatus() {
        return jda.getStatus().name();
    }

    @ManagedAttribute(description = "Returns the total amount of JSON responses that discord has sent.")
    public long getResponseTotal() {
        return jda.getResponseTotal();
    }

    @ManagedAttribute(description = "Returns the guild count handled by this shard")
    public long getGuildCount() {
        return jda.getGuildCache().size();
    }

    @ManagedAttribute(description = "Returns the text channels count handled by this shard")
    public long getTextChannelsCount() {
        return jda.getTextChannelCache().size();
    }

    @ManagedAttribute(description = "Returns the voice channels count handled by this shard")
    public long getVoiceChannelCount() {
        return jda.getVoiceChannelCache().size();
    }

    /* =====================================================
                         RATE LIMIT POOL
       ===================================================== */

    @ManagedAttribute(description = "[Rate-Limit Pool] Returns the number of threads that execute tasks")
    public int getRatePoolActiveCount() {
        return rateLimitPool != null ? rateLimitPool.getActiveCount() : 0;
    }

    @ManagedAttribute(description = "[Rate-Limit Pool] Return the current pool size")
    public int getRatePoolSize() {
        return rateLimitPool != null ? rateLimitPool.getPoolSize() : 0;
    }

    @ManagedAttribute(description = "[Rate-Limit Pool] Returns the size of the core pool of threads")
    public int getRateCorePoolSize() {
        return rateLimitPool != null ? rateLimitPool.getCorePoolSize() : 0;
    }

    @ManagedAttribute(description = "[Rate-Limit Pool] Sets the core size of the pool")
    public void seRatetCorePoolSize(int corePoolSize) {
        if (rateLimitPool != null) {
            rateLimitPool.setCorePoolSize(corePoolSize);
        }
    }

    @ManagedAttribute(description = "[Rate-Limit Pool] Returns the max size allowed in the pool of threads")
    public int getRateMaxPoolSize() {
        return rateLimitPool != null ? rateLimitPool.getMaximumPoolSize() : 0;
    }

    @ManagedAttribute(description = "[Rate-Limit Pool] Sets the max size allowed in the pool of threads")
    public void setRateMaxPoolSize(int maxPoolSize) {
        if (rateLimitPool != null) {
            rateLimitPool.setMaximumPoolSize(maxPoolSize);
        }
    }

    @ManagedAttribute(description = "[Rate-Limit Pool] Returns the total number of completed tasks")
    public long getRatePoolCompletedTaskCount() {
        return rateLimitPool != null ? rateLimitPool.getCompletedTaskCount() : 0;
    }

    @ManagedAttribute(description = "[Rate-Limit Pool] Returns the largest number of threads that have been in the pool")
    public int getRateLargestPoolSize() {
        return rateLimitPool != null ? rateLimitPool.getLargestPoolSize() : 0;
    }

    @ManagedAttribute(description = "[Rate-Limit Pool] Returns the size of current queue")
    public int getRatePoolQueueSize() {
        return rateLimitPool != null ? rateLimitPool.getQueue().size() : 0;
    }

    @ManagedAttribute(description = "[Rate-Limit Pool] Returns the number of additional elements that this queue can "
            + "accept without  "
            + "blocking")
    public int getRatePoolQueueRemainingCapacity() {
        return rateLimitPool != null ? rateLimitPool.getQueue().remainingCapacity() : 0;
    }

    @ManagedAttribute(description = "[Rate-Limit Pool] Returns the total number of tasks that have ever been scheduled for execution ")
    public long getRatePoolTaskCount() {
        return rateLimitPool != null ? rateLimitPool.getTaskCount() : 0;
    }

    @Override
    public String getJmxName() {
        return jda.getShardInfo().toString();
    }

    @Override
    public String[] getJmxPath() {
        return new String[]{"JDA"};
    }
}
