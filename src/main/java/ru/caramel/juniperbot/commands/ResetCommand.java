package ru.caramel.juniperbot.commands;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.PermissionException;
import org.springframework.beans.factory.annotation.Autowired;
import ru.caramel.juniperbot.integration.discord.DiscordWebHookPoster;
import ru.caramel.juniperbot.model.BotContext;

@DiscordCommand(key = "сбросвх", description = "Сбросить пост вебхуков до последнего", hidden = true)
public class ResetCommand implements Command {

    @Autowired
    private DiscordWebHookPoster poster;

    @Override
    public boolean doCommand(MessageReceivedEvent message, BotContext context) {
        poster.setResetToSecond(true);
        try {
            message.getChannel().sendMessage("Сброшено.").queue();
        } catch (PermissionException e) {
            // fall down
        }
        return true;
    }
}
