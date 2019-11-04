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
package ru.juniperbot.common.worker.jobs;

import lombok.NonNull;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.apache.commons.lang3.StringUtils;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import ru.juniperbot.common.worker.shared.service.DiscordService;
import ru.juniperbot.common.worker.shared.support.AbstractJob;

import java.util.concurrent.TimeUnit;

public class UnBanJob extends AbstractJob {

    public static final String ATTR_USER_ID = "userId";
    public static final String ATTR_GUILD_ID = "guildId";
    public static final String GROUP = "UnBanJob-group";

    @Autowired
    private DiscordService discordService;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        ShardManager shardManager = discordService.getShardManager();
        if (shardManager == null || !discordService.isConnected()) {
            reschedule(jobExecutionContext, TimeUnit.MINUTES, 10);
            return;
        }

        JobDataMap data = jobExecutionContext.getJobDetail().getJobDataMap();

        String guildId = data.getString(ATTR_GUILD_ID);
        String userId = data.getString(ATTR_USER_ID);

        if (StringUtils.isEmpty(guildId) || StringUtils.isEmpty(userId)) {
            return;
        }

        Guild guild = shardManager.getGuildById(guildId);
        if (guild != null) {
            if (!guild.isAvailable()) {
                reschedule(jobExecutionContext, TimeUnit.MINUTES, 10);
                return;
            }
            if (guild.getSelfMember().hasPermission(Permission.BAN_MEMBERS)) {
                guild.unban(userId).queue();
            }
        }
    }

    public static JobDetail createDetails(@NonNull String guildId, @NonNull String userId) {
        return JobBuilder
                .newJob(UnBanJob.class)
                .withIdentity(getKey(guildId, userId))
                .usingJobData(ATTR_GUILD_ID, guildId)
                .usingJobData(ATTR_USER_ID, userId)
                .build();
    }

    public static JobKey getKey(@NonNull String guildId, @NonNull String userId) {
        String identity = String.format("%s-%s-%s", GROUP, guildId, userId);
        return new JobKey(identity, GROUP);
    }
}
