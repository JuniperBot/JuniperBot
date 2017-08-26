package ru.caramel.juniperbot.commands.audio;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import ru.caramel.juniperbot.commands.model.BotContext;
import ru.caramel.juniperbot.commands.model.CommandGroup;
import ru.caramel.juniperbot.commands.model.CommandSource;
import ru.caramel.juniperbot.commands.model.DiscordCommand;
import ru.caramel.juniperbot.integration.discord.model.DiscordException;

@DiscordCommand(
        key = "старт",
        description = "Восстановить воспроизведение текущего трека",
        source = CommandSource.GUILD,
        group = CommandGroup.MUSIC,
        priority = 106)
public class StartCommand extends AudioCommand {

    @Override
    public boolean doInternal(MessageReceivedEvent message, BotContext context, String content) throws DiscordException {
        if (!handlerService.resumeTrack(message.getGuild())) {
            messageManager.onMessage(message.getChannel(), "Воспроизведение не запущено");
        }
        return true;
    }
}
