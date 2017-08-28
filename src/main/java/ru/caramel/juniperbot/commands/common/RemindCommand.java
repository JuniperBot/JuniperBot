package ru.caramel.juniperbot.commands.common;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import ru.caramel.juniperbot.commands.Command;
import ru.caramel.juniperbot.commands.model.DiscordCommand;
import ru.caramel.juniperbot.commands.model.BotContext;
import ru.caramel.juniperbot.integration.discord.model.DiscordException;
import ru.caramel.juniperbot.service.MessageService;
import ru.caramel.juniperbot.service.ReminderService;
import ru.caramel.juniperbot.utils.TimeSequenceParser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@DiscordCommand(key = "напомни", description = "Напомнить о чем-либо. Введите команду без аргументов для полной справки", priority = 2)
public class RemindCommand implements Command {

    private static final DateTimeFormatter FORMATTER = DateTimeFormat.forPattern("dd.MM.yyyy HH:mm").withZone(DateTimeZone.forID("Europe/Moscow"));

    private static final Pattern PATTERN = Pattern.compile("^(\\d{2}\\.\\d{2}\\.\\d{4})\\s+(\\d{2}:\\d{2})\\s+(.*)$");

    private static final Pattern RELATIVE_PATTERN = Pattern.compile("^\\s*через\\s+(.*)\\s+(.*)$");

    private static final TimeSequenceParser SEQUENCE_PARSER = new TimeSequenceParser();

    @Autowired
    private ReminderService reminderService;

    @Autowired
    private MessageService messageService;

    @Override
    public boolean doCommand(MessageReceivedEvent message, BotContext context, String content) throws DiscordException {

        try {
            DateTime date = null;
            String reminder = null;
            Matcher m = PATTERN.matcher(content);
            if (m.find()) {
                date = FORMATTER.parseDateTime(String.format("%s %s", m.group(1), m.group(2)));
                reminder = m.group(3);
                if (DateTime.now().isAfter(date)) {
                    message.getChannel().sendMessage("Указывай дату в будущем, пожалуйста").queue();
                    return false;
                }
            }

            m = RELATIVE_PATTERN.matcher(content);
            if (m.find()) {
                Long millis = SEQUENCE_PARSER.parse(m.group(1));
                if (millis != null) {
                    reminder = m.group(2);
                    date = DateTime.now().plus(millis);
                }
            }

            if (date != null && reminder != null) {
                messageService.onMessage(message.getChannel(), "Лаааадно, напомню. Фыр.");
                reminderService.createReminder(message.getChannel(), message.getMember(), reminder, date.toDate());
                return true;
            }
        } catch (IllegalArgumentException e) {
            // fall down
        }
        return printHelp(message, context);
    }

    private boolean printHelp(MessageReceivedEvent message, BotContext context) {
        DateTime current = DateTime.now();
        current = current.plusMinutes(1);
        EmbedBuilder builder = messageService.getBaseEmbed();
        builder.setTitle("Пример использования команды напоминания:");
        builder.addField("Использование даты в формате дд.ММ.гггг чч:мм", String.format("Например: `%sнапомни %s фыр!`", context.getPrefix(), FORMATTER.print(current)), false);
        builder.addField("Использование выражения \"через\"", String.format("```\n" +
                "%1$sнапомни через 60 секунд фыр!\n" +
                "%1$sнапомни через 5 минут фыр!\n" +
                "%1$sнапомни через 5 минут фыр!\n" +
                "%1$sнапомни через 1 час 30 минут фыр!\n" +
                "%1$sнапомни через 2 недели и 5 дней фыр!\n" +
                "```\n" +
                "Поддерживаются: месяц, неделя, дни, часы, минуты, секунды и даже миллисекунды о_О", context.getPrefix()), false);
        messageService.sendMessageSilent(message.getChannel()::sendMessage, builder.build());
        return false;
    }
}
