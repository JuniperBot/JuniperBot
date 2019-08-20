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
package ru.juniperbot.worker.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import ru.juniperbot.worker.common.command.model.AbstractCommand;
import ru.juniperbot.worker.common.command.model.BotContext;
import ru.juniperbot.worker.common.command.model.DiscordCommand;
import ru.juniperbot.common.service.ConfigService;
import ru.juniperbot.common.utils.CommonUtils;
import ru.caramel.juniperbot.module.reminder.jobs.ReminderJob;
import ru.juniperbot.common.utils.TimeSequenceParser;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@DiscordCommand(key = "discord.command.remind.key",
        description = "discord.command.remind.desc",
        group = "discord.command.group.utility",
        priority = 1)
public class RemindCommand extends AbstractCommand {

    private static final Pattern PATTERN = Pattern.compile("^(\\d{2}\\.\\d{2}\\.\\d{4})\\s+(\\d{2}:\\d{2})\\s+(.*)$");

    private static final String RELATIVE_PATTERN_FORMAT = "^\\s*%s\\s+(\\d+\\s+[a-zA-Zа-яА-Я]+(?:\\s+\\d+\\s+[a-zA-Zа-яА-Я]+)*)\\s+(.*)$";

    private static final TimeSequenceParser SEQUENCE_PARSER = new TimeSequenceParser();

    @Autowired
    private SchedulerFactoryBean schedulerFactoryBean;

    @Autowired
    private ConfigService configService;

    @Override
    public boolean doCommand(GuildMessageReceivedEvent message, BotContext context, String content) {
        try {
            DateTime date = null;
            String reminder = null;
            Matcher m = PATTERN.matcher(content);
            if (m.find()) {
                date = getFormatter(context).parseDateTime(String.format("%s %s", m.group(1), m.group(2)));
                reminder = m.group(3);
                if (DateTime.now().isAfter(date)) {
                    messageService.onError(message.getChannel(), "discord.command.remind.error.future");
                    return fail(message);
                }
            }

            String keyWord = messageService.getMessage("discord.command.remind.keyWord");
            m = Pattern.compile(String.format(RELATIVE_PATTERN_FORMAT, keyWord)).matcher(content);
            if (m.find()) {
                Long millis = SEQUENCE_PARSER.parse(m.group(1));
                reminder = m.group(2);
                if (millis != null && StringUtils.isNotEmpty(reminder)) {
                    date = DateTime.now().plus(millis);
                }
            }

            if (date != null && reminder != null) {
                createReminder(message.getChannel(), message.getMember(), reminder, date.toDate());
                return ok(message, "discord.command.remind.done");
            }
        } catch (IllegalArgumentException e) {
            // fall down
        }

        String prefix = context.getConfig() != null ? context.getConfig().getPrefix() : configService.getDefaultPrefix();

        String remindCommand = messageService.getMessageByLocale("discord.command.remind.key",
                context.getCommandLocale());

        DateTime current = DateTime.now();
        current = current.plusMinutes(1);
        EmbedBuilder builder = messageService.getBaseEmbed();
        builder.setTitle(messageService.getMessage("discord.command.remind.help.title"));
        builder.addField(
                messageService.getMessage("discord.command.remind.help.field1.title",
                        CommonUtils.getUTCOffset(context.getTimeZone())),
                messageService.getMessage("discord.command.remind.help.field1.value", prefix, remindCommand,
                        getFormatter(context).print(current)), false);
        builder.addField(
                messageService.getMessage("discord.command.remind.help.field2.title"),
                messageService.getMessage("discord.command.remind.help.field2.value", prefix, remindCommand),
                false);
        messageService.sendMessageSilent(message.getChannel()::sendMessage, builder.build());
        return false;
    }

    private DateTimeFormatter getFormatter(BotContext context) {
        return DateTimeFormat.forPattern("dd.MM.yyyy HH:mm").withZone(context.getTimeZone());
    }

    private void createReminder(MessageChannel channel, Member member, String message, Date date) {
        JobDetail job = ReminderJob.createDetails(channel, member, message);
        Trigger trigger = TriggerBuilder
                .newTrigger()
                .startAt(date)
                .withSchedule(SimpleScheduleBuilder.simpleSchedule())
                .build();
        try {
            schedulerFactoryBean.getScheduler().scheduleJob(job, trigger);
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }
    }
}
