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
package ru.caramel.juniperbot.module.reminder.jobs;

import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.apache.commons.lang3.StringUtils;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import ru.juniperbot.worker.common.shared.service.DiscordService;
import ru.juniperbot.worker.common.moderation.service.ModerationService;
import ru.juniperbot.worker.common.shared.support.AbstractJob;
import ru.juniperbot.worker.common.utils.DiscordUtils;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class ReminderJob extends AbstractJob {

    private static final String ATTR_USER_ID = "userId";
    private static final String ATTR_GUILD_ID = "guildId";
    private static final String ATTR_CHANNEL_ID = "channelId";
    private static final String ATTR_MESSAGE = "message";
    private static final String GROUP = "ReminderJob-group";

    @Autowired
    private DiscordService discordService;

    @Autowired
    private ModerationService moderationService;

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

        if (StringUtils.isEmpty(userId) || StringUtils.isEmpty(guildId) || StringUtils.isEmpty(channelId)) {
            return;
        }

        User user = shardManager.getUserById(userId);
        if (user == null) {
            return;
        }

        Guild guild = shardManager.getGuildById(guildId);
        if (guild == null || !guild.isAvailable()) {
            return;
        }

        TextChannel channel = guild.getTextChannelById(channelId);
        if (channel == null) {
            return;
        }

        boolean maskMentions = true;
        String messageRaw = data.getString(ATTR_MESSAGE);

        StringBuilder message = new StringBuilder();
        if (guild.isMember(user)) {
            maskMentions = !moderationService.isModerator(guild.getMember(user));
            message.append(user.getAsMention()).append(" ");
        }
        if (maskMentions) {
            messageRaw = DiscordUtils.maskPublicMentions(messageRaw);
        }
        message.append(messageRaw);
        channel.sendMessage(message.toString()).queue();
    }

    public static JobDetail createDetails(MessageChannel channel, Member member, String message) {
        String userId;
        String guildId = null;
        if (channel instanceof PrivateChannel) {
            userId = ((PrivateChannel) channel).getUser().getId();
        } else {
            guildId = member.getGuild().getId();
            userId = member.getUser().getId();
        }
        return JobBuilder
                .newJob(ReminderJob.class)
                .withIdentity(GROUP + " - " + UUID.randomUUID(), GROUP)
                .usingJobData(ATTR_GUILD_ID, guildId)
                .usingJobData(ATTR_USER_ID, userId)
                .usingJobData(ATTR_CHANNEL_ID, channel.getId())
                .usingJobData(ATTR_MESSAGE, message)
                .build();
    }
}
