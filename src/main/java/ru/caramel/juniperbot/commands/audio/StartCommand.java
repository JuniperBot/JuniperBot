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
        key = "старт",
        description = "Восстановить воспроизведение текущего трека",
        source = CommandSource.GUILD,
        group = CommandGroup.MUSIC)
public class StartCommand implements Command {

    @Autowired
    private PlaybackManager playbackManager;

    @Autowired
    private MessageManager messageManager;

    @Override
    public boolean doCommand(MessageReceivedEvent message, BotContext context, String content) throws DiscordException {
        if (!playbackManager.validateChannel(message.getTextChannel(), message.getAuthor())) {
            return false;
        }
        if (!playbackManager.resumeTrack(message.getGuild())) {
            messageManager.onMessage(message.getChannel(), "Воспроизведение не запущено");
        }
        return true;
    }
}
