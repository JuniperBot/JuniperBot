package ru.caramel.juniperbot.commands;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import ru.caramel.juniperbot.integration.discord.DiscordWebHookPoster;
import ru.caramel.juniperbot.model.BotContext;

@DiscordCommand(key = "сбросвх", description = "Сбросить пост вебхуков до последнего", hidden = true)
public class ResetCommand implements Command {

    @Autowired
    private DiscordWebHookPoster poster;

    @Override
    public boolean doCommand(MessageReceivedEvent message, BotContext context, String[] args) {
        poster.setResetToSecond(true);
        message.getChannel().sendMessage("Сброшено.").submit();
        return true;
    }
}
