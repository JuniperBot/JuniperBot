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
package ru.juniperbot.common.worker.jobs;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.apache.commons.lang3.StringUtils;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import ru.juniperbot.common.worker.modules.moderation.service.MuteService;
import ru.juniperbot.common.worker.shared.service.DiscordService;
import ru.juniperbot.common.worker.shared.support.AbstractJob;

import java.util.concurrent.TimeUnit;

public class UnMuteJob extends AbstractJob {

    public static final String ATTR_USER_ID = "userId";
    public static final String ATTR_GUILD_ID = "guildId";
    public static final String ATTR_GLOBAL_ID = "global";
    public static final String ATTR_CHANNEL_ID = "channelId";
    public static final String GROUP = "UnMuteJob-group";

    @Autowired
    private DiscordService discordService;

    @Autowired
    private MuteService muteService;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        ShardManager shardManager = discordService.getShardManager();
        if (shardManager == null || !discordService.isConnected()) {
            reschedule(jobExecutionContext, TimeUnit.MINUTES, 1);
            return;
        }

        JobDataMap data = jobExecutionContext.getJobDetail().getJobDataMap();

        String userId = data.getString(ATTR_USER_ID);
        String guildId = data.getString(ATTR_GUILD_ID);
        String channelId = data.getString(ATTR_CHANNEL_ID);

        if (guildId != null && userId != null && channelId != null) {
            muteService.clearState(Long.parseLong(guildId), userId, channelId);
        }

        if (StringUtils.isEmpty(guildId)) {
            return;
        }

        Guild guild = shardManager.getGuildById(guildId);
        if (guild != null) {
            if (!guild.isAvailable()) {
                reschedule(jobExecutionContext, TimeUnit.MINUTES, 1);
                return;
            }
            Member member = guild.getMemberById(userId);
            if (member != null) {
                TextChannel channel = StringUtils.isNotBlank(channelId) ? guild.getTextChannelById(channelId) : null;
                muteService.unmute(null, channel, member);
            }
        }
    }

    public static JobDetail createDetails(boolean global, TextChannel channel, Member member) {
        return JobBuilder
                .newJob(UnMuteJob.class)
                .withIdentity(channel != null ? getKey(member, channel) : getKey(member))
                .usingJobData(ATTR_GUILD_ID, member.getGuild().getId())
                .usingJobData(ATTR_GLOBAL_ID, String.valueOf(global))
                .usingJobData(ATTR_USER_ID, member.getUser().getId())
                .usingJobData(ATTR_CHANNEL_ID, channel != null ? channel.getId() : null)
                .build();
    }

    public static JobKey getKey(Member member) {
        String identity = String.format("%s-%s-%s", GROUP, member.getGuild().getId(), member.getUser().getId());
        return new JobKey(identity, GROUP);
    }

    public static JobKey getKey(Member member, TextChannel channel) {
        String identity = String.format("%s-%s-%s-%s",
                GROUP,
                member.getGuild().getId(),
                member.getUser().getId(),
                channel.getId());
        return new JobKey(identity, GROUP);
    }
}
