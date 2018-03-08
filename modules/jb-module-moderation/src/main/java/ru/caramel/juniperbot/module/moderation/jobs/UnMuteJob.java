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
package ru.caramel.juniperbot.module.moderation.jobs;

import net.dv8tion.jda.bot.sharding.ShardManager;
import net.dv8tion.jda.core.entities.*;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import ru.caramel.juniperbot.core.service.DiscordService;
import ru.caramel.juniperbot.core.support.AbstractJob;
import ru.caramel.juniperbot.module.moderation.service.ModerationService;

import java.util.concurrent.TimeUnit;

public class UnMuteJob extends AbstractJob {

    private static final String ATTR_USER_ID = "userId";
    private static final String ATTR_GUILD_ID = "guildId";
    private static final String ATTR_CHANNEL_ID = "channelId";
    private static final String GROUP = "UnMuteJob-group";

    @Autowired
    private DiscordService discordService;

    @Autowired
    private ModerationService moderationService;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        ShardManager shardManager = discordService.getShardManager();
        if (shardManager == null || !discordService.isConnected()) {
            reschedule(jobExecutionContext, TimeUnit.MINUTES, 1);
            return;
        }

        JobDataMap data = jobExecutionContext.getJobDetail().getJobDataMap();

        String userId = data.getString(ATTR_USER_ID);
        String guildId = data.getString(ATTR_GUILD_ID);
        String channelId = data.getString(ATTR_CHANNEL_ID);

        Guild guild = shardManager.getGuildById(guildId);
        if (guild != null) {
            if (!guild.isAvailable()) {
                reschedule(jobExecutionContext, TimeUnit.MINUTES, 1);
                return;
            }
            Member member = guild.getMemberById(userId);
            if (member != null) {
                moderationService.unmute(guild.getTextChannelById(channelId), member);
            }
        }
    }

    public static JobDetail createDetails(TextChannel channel, Member member) {
        return JobBuilder
                .newJob(UnMuteJob.class)
                .withIdentity(getKey(member))
                .usingJobData(ATTR_GUILD_ID, member.getGuild().getId())
                .usingJobData(ATTR_USER_ID, member.getUser().getId())
                .usingJobData(ATTR_CHANNEL_ID, channel.getId())
                .build();
    }

    public static JobKey getKey(Member member) {
        String identity = String.format("%s-%s-%s", GROUP, member.getGuild().getId(), member.getUser().getId());
        return new JobKey(identity, GROUP);
    }
}
