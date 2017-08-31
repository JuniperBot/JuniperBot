package ru.caramel.juniperbot.commands.audio.queue;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import ru.caramel.juniperbot.commands.audio.AudioCommand;
import ru.caramel.juniperbot.commands.model.CommandGroup;
import ru.caramel.juniperbot.commands.model.DiscordCommand;
import ru.caramel.juniperbot.commands.model.BotContext;
import ru.caramel.juniperbot.commands.model.CommandSource;
import ru.caramel.juniperbot.integration.discord.model.DiscordException;

@DiscordCommand(
        key = "discord.command.skip.key",
        description = "discord.command.skip.desc",
        source = CommandSource.GUILD,
        group = CommandGroup.MUSIC,
        priority = 110)
public class SkipCommand extends AudioCommand {

    @Override
    public boolean doInternal(MessageReceivedEvent message, BotContext context, String content) throws DiscordException {
        playerService.skipTrack(message.getGuild());
        return true;
    }
}
