package ru.caramel.juniperbot.commands;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import ru.caramel.juniperbot.configuration.DiscordConfig;
import ru.caramel.juniperbot.model.BotContext;
import ru.caramel.juniperbot.model.exception.DiscordException;

@DiscordCommand(key = "напомни", description = "Напомнить о чем-либо. Дата в формате дд.ММ.гггг чч:мм и сообщение в кавычках")
public class RemindCommand extends ParameterizedCommand {

    private static final DateTimeFormatter FORMATTER = DateTimeFormat.forPattern("dd.MM.yyyy HH:mm").withZone(DateTimeZone.forID("Europe/Moscow"));

    @Autowired
    private TaskScheduler taskScheduler;

    @Autowired
    private DiscordConfig discordConfig;

    @Override
    public boolean doCommand(MessageReceivedEvent message, BotContext context, String[] args) throws DiscordException {
        if (args.length < 2) {
            return printHelp(message);
        }

        try {
            DateTime date = FORMATTER.parseDateTime(args[0]);
            if (DateTime.now().isAfter(date)) {
                message.getChannel().sendMessage("Указывай дату в будущем, пожалуйста").queue();
                return false;
            }
            taskScheduler.schedule(() -> {
                message.getChannel().sendMessage(args[1]).queue();
            }, date.toDate());
            message.getChannel().sendMessage("Лаааадно, напомню. Фыр.").queue();
        } catch (IllegalArgumentException e) {
            return printHelp(message);
        }
        return true;
    }

    private boolean printHelp(MessageReceivedEvent message) {
        message.getChannel().sendMessage(String.format("Дата в формате дд.ММ.гггг чч:мм и сообщение в кавычках. Например: %sнапомни \"03.07.2017 21:27\" \"Сообщение\"",
                discordConfig.getPrefix())).queue();
        return false;
    }
}
