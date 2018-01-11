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
package ru.caramel.juniperbot.core.modules.reminder.jobs;

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.*;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import ru.caramel.juniperbot.core.service.DiscordService;

import java.util.UUID;

public class ReminderJob implements Job {

    private static final String ATTR_USER_ID = "userId";
    private static final String ATTR_GUILD_ID = "guildId";
    private static final String ATTR_CHANNEL_ID = "channelId";
    private static final String ATTR_MESSAGE = "message";
    private static final String GROUP = "ReminderJob-group";

    @Autowired
    private DiscordService discordService;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        JDA jda = discordService.getJda();
        if (jda == null || !JDA.Status.CONNECTED.equals(jda.getStatus())) {
            throw new RuntimeException("Could not send reminder, not connected!");
        }

        JobDataMap data = jobExecutionContext.getJobDetail().getJobDataMap();

        String userId = data.getString(ATTR_USER_ID);
        String guildId = data.getString(ATTR_GUILD_ID);
        String channelId = data.getString(ATTR_CHANNEL_ID);
        String messageRaw = data.getString(ATTR_MESSAGE);

        MessageChannel channel = null;
        User user = jda.getUserById(userId);
        StringBuilder message = new StringBuilder();
        if (guildId != null) {
            Guild guild = jda.getGuildById(guildId);
            if (guild != null) {
                channel = guild.getTextChannelById(channelId);
                if (user != null && guild.isMember(user)) {
                    message.append(user.getAsMention()).append(" ");
                }
            }
        } else {
            channel = jda.getPrivateChannelById(channelId);
        }
        if (channel == null && user != null) {
            channel = user.openPrivateChannel().complete();
        }
        if (channel != null) {
            message.append(messageRaw);
            channel.sendMessage(message.toString()).queue();
        }
    }

    public static JobDetail createDetails(MessageChannel channel, Member member, String message) {
        String userId;
        String guildId = null;
        if (channel instanceof PrivateChannel) {
            userId = ((PrivateChannel)channel).getUser().getId();
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
