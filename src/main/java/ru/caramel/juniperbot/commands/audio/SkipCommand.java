package ru.caramel.juniperbot.commands.audio;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import ru.caramel.juniperbot.audio.service.PlaybackManager;
import ru.caramel.juniperbot.commands.base.Command;
import ru.caramel.juniperbot.commands.model.CommandGroup;
import ru.caramel.juniperbot.commands.model.DiscordCommand;
import ru.caramel.juniperbot.commands.model.BotContext;
import ru.caramel.juniperbot.commands.model.CommandSource;
import ru.caramel.juniperbot.integration.discord.model.DiscordException;

@DiscordCommand(
        key = "скип",
        description = "Перейти к воспроизведению следующего трека",
        source = CommandSource.GUILD,
        group = CommandGroup.MUSIC)
public class SkipCommand implements Command {

    @Autowired
    private PlaybackManager playbackManager;

    @Override
    public boolean doCommand(MessageReceivedEvent message, BotContext context) throws DiscordException {
        playbackManager.skipTrack(message.getGuild());
        return true;
    }
}
