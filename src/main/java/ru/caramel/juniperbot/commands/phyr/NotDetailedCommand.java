package ru.caramel.juniperbot.commands.phyr;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import ru.caramel.juniperbot.commands.Command;
import ru.caramel.juniperbot.commands.DiscordCommand;
import ru.caramel.juniperbot.model.BotContext;
import ru.caramel.juniperbot.model.exception.DiscordException;

@DiscordCommand(key = "фырно", description = "Фырчать фырные картинки без лишних нефыров")
public class NotDetailedCommand implements Command {

    @Override
    public boolean doCommand(MessageReceivedEvent message, BotContext context) throws DiscordException {
        if (!context.isDetailedEmbed()) {
            message.getChannel().sendMessage("Уже фырнее некуда! ^_^").queue();
            return true;
        }
        context.setDetailedEmbed(false);
        message.getChannel().sendMessage("Пофырчим! <^.^>").queue();
        return true;
    }
}
