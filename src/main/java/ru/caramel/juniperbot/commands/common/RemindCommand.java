package ru.caramel.juniperbot.commands.common;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.PermissionException;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import ru.caramel.juniperbot.commands.Command;
import ru.caramel.juniperbot.commands.model.DiscordCommand;
import ru.caramel.juniperbot.commands.model.BotContext;
import ru.caramel.juniperbot.integration.discord.model.DiscordException;
import ru.caramel.juniperbot.service.ReminderService;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@DiscordCommand(key = "напомни", description = "Напомнить о чем-либо. Дата в формате дд.ММ.гггг чч:мм и сообщение")
public class RemindCommand implements Command {

    private static final DateTimeFormatter FORMATTER = DateTimeFormat.forPattern("dd.MM.yyyy HH:mm").withZone(DateTimeZone.forID("Europe/Moscow"));

    private static Pattern PATTERN = Pattern.compile("^(\\d{2}\\.\\d{2}\\.\\d{4})\\s+(\\d{2}:\\d{2})\\s+(.*)$");

    @Autowired
    private ReminderService reminderService;

    @Override
    public boolean doCommand(MessageReceivedEvent message, BotContext context, String content) throws DiscordException {
        Matcher m = PATTERN.matcher(content);
        if (!m.find()) {
            return printHelp(message, context);
        }

        try {
            DateTime date = FORMATTER.parseDateTime(String.format("%s %s", m.group(1), m.group(2)));
            if (DateTime.now().isAfter(date)) {
                message.getChannel().sendMessage("Указывай дату в будущем, пожалуйста").queue();
                return false;
            }
            reminderService.createReminder(message.getChannel(), message.getMember(), m.group(3), date.toDate());
            message.getChannel().sendMessage("Лаааадно, напомню. Фыр.").queue();
        } catch (PermissionException e) {
            return false;
        } catch (IllegalArgumentException e) {
            return printHelp(message, context);
        }
        return true;
    }

    private boolean printHelp(MessageReceivedEvent message, BotContext context) {
        DateTime current = DateTime.now();
        current = current.plusMinutes(1);
        message.getChannel().sendMessage(String.format("Дата в формате дд.ММ.гггг чч:мм и сообщение. Например: `%sнапомни %s сообщение`",
                context.getPrefix(), FORMATTER.print(current))).queue();
        return false;
    }
}
