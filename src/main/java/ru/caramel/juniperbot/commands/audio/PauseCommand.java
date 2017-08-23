package ru.caramel.juniperbot.commands.audio;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import ru.caramel.juniperbot.audio.service.MessageManager;
import ru.caramel.juniperbot.audio.service.PlaybackManager;
import ru.caramel.juniperbot.commands.base.Command;
import ru.caramel.juniperbot.commands.model.BotContext;
import ru.caramel.juniperbot.commands.model.CommandGroup;
import ru.caramel.juniperbot.commands.model.CommandSource;
import ru.caramel.juniperbot.commands.model.DiscordCommand;
import ru.caramel.juniperbot.integration.discord.model.DiscordException;

@DiscordCommand(
        key = "пауза",
        description = "Приостановить воспроизведение текущего трека",
        source = CommandSource.GUILD,
        group = CommandGroup.MUSIC)
public class PauseCommand implements Command {

    @Autowired
    private PlaybackManager playbackManager;

    @Autowired
    private MessageManager messageManager;

    @Override
    public boolean doCommand(MessageReceivedEvent message, BotContext context, String content) throws DiscordException {
        if (!playbackManager.validateChannel(message.getTextChannel(), message.getAuthor())) {
            return false;
        }
        messageManager.onMessage(message.getChannel(), playbackManager.pauseTrack(message.getGuild())
                ? "Воспроизведение приостановлено :pause_button: " : "Воспроизведение не запущено");
        return true;
    }
}
