package ru.caramel.juniperbot.commands.audio.control;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import ru.caramel.juniperbot.commands.audio.AudioCommand;
import ru.caramel.juniperbot.commands.model.BotContext;
import ru.caramel.juniperbot.commands.model.CommandGroup;
import ru.caramel.juniperbot.commands.model.CommandSource;
import ru.caramel.juniperbot.commands.model.DiscordCommand;
import ru.caramel.juniperbot.integration.discord.model.DiscordException;

@DiscordCommand(
        key = "пауза",
        description = "Приостановить воспроизведение текущего трека",
        source = CommandSource.GUILD,
        group = CommandGroup.MUSIC,
        priority = 105)
public class PauseCommand extends AudioCommand {

    @Override
    public boolean doInternal(MessageReceivedEvent message, BotContext context, String content) throws DiscordException {
        messageManager.onMessage(message.getChannel(), handlerService.pauseTrack(message.getGuild())
                ? "Воспроизведение приостановлено :pause_button: " : "Воспроизведение не запущено");
        return true;
    }
}
