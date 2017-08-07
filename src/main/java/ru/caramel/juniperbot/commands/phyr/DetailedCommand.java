package ru.caramel.juniperbot.commands.phyr;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import ru.caramel.juniperbot.commands.Command;
import ru.caramel.juniperbot.commands.DiscordCommand;
import ru.caramel.juniperbot.model.BotContext;
import ru.caramel.juniperbot.model.exception.DiscordException;

@DiscordCommand(key = "нефырно", description = "Фырчать фырные картинки с непонятными надписями")
public class DetailedCommand implements Command {

    @Override
    public boolean doCommand(MessageReceivedEvent message, BotContext context) throws DiscordException {
        if (context.isDetailedEmbed()) {
            context.getChannel().sendMessage("Тебе мало буков? >_>").queue();
            return true;
        }
        context.setDetailedEmbed(true);
        context.getChannel().sendMessage("Ну фыыыр :C").queue();
        return true;
    }
}
