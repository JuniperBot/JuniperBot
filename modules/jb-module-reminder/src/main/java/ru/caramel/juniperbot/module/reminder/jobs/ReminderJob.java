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

import net.dv8tion.jda.bot.sharding.ShardManager;
import net.dv8tion.jda.core.entities.*;
import org.apache.commons.lang3.StringUtils;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import ru.caramel.juniperbot.core.service.DiscordService;
import ru.caramel.juniperbot.core.support.AbstractJob;
import ru.caramel.juniperbot.core.utils.CommonUtils;
import ru.caramel.juniperbot.module.moderation.service.ModerationService;

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
        String messageRaw = data.getString(ATTR_MESSAGE);

        boolean maskMentions = true;

        MessageChannel channel = null;
        User user = shardManager.getUserById(userId);
        StringBuilder message = new StringBuilder();
        if (StringUtils.isNotEmpty(guildId)) {
            Guild guild = shardManager.getGuildById(guildId);
            if (guild != null && guild.isAvailable()) {
                channel = guild.getTextChannelById(channelId);
                if (user != null && guild.isMember(user)) {
                    maskMentions = !moderationService.isModerator(guild.getMember(user));
                    message.append(user.getAsMention()).append(" ");
                }
            }
        } else {
            channel = shardManager.getPrivateChannelById(channelId);
        }
        if (maskMentions) {
            messageRaw = CommonUtils.maskPublicMentions(messageRaw);
        }
        message.append(messageRaw);
        if (channel == null && user != null) {
            user.openPrivateChannel().queue(c -> c.sendMessage(message.toString()).queue());
        } else if (channel != null) {
            channel.sendMessage(message.toString()).queue();
        }
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
