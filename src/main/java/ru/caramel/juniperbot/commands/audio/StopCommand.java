package ru.caramel.juniperbot.commands.audio;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import ru.caramel.juniperbot.commands.model.BotContext;
import ru.caramel.juniperbot.commands.model.CommandGroup;
import ru.caramel.juniperbot.commands.model.CommandSource;
import ru.caramel.juniperbot.commands.model.DiscordCommand;
import ru.caramel.juniperbot.integration.discord.model.DiscordException;

@DiscordCommand(
        key = "стоп",
        description = "Остановить воспроизведение с очисткой плейлиста",
        source = CommandSource.GUILD,
        group = CommandGroup.MUSIC)
public class StopCommand extends AudioCommand {

    @Override
    public boolean doInternal(MessageReceivedEvent message, BotContext context, String content) throws DiscordException {
        messageManager.onMessage(message.getChannel(), handlerService.stop(message.getGuild())
                ? "Воспроизведение остановлено :stop_button: " :"Воспроизведение не запущено");
        return true;
    }
}
