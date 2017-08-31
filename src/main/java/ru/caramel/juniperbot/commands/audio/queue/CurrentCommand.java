package ru.caramel.juniperbot.commands.audio.queue;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import ru.caramel.juniperbot.audio.model.TrackRequest;
import ru.caramel.juniperbot.commands.audio.AudioCommand;
import ru.caramel.juniperbot.commands.model.BotContext;
import ru.caramel.juniperbot.commands.model.CommandGroup;
import ru.caramel.juniperbot.commands.model.CommandSource;
import ru.caramel.juniperbot.commands.model.DiscordCommand;
import ru.caramel.juniperbot.integration.discord.model.DiscordException;

@DiscordCommand(
        key = "текущая",
        description = "Показать текущую воспроизводимую композицию",
        source = CommandSource.GUILD,
        group = CommandGroup.MUSIC,
        priority = 102)
public class CurrentCommand extends AudioCommand {
    @Override
    protected boolean doInternal(MessageReceivedEvent message, BotContext context, String content) throws DiscordException {

        TrackRequest current = playerService.getInstance(message.getGuild()).getCurrent();
        if (current == null) {
            messageManager.onMessage(message.getChannel(), "discord.command.audio.notStarted");
            return false;
        }
        current.setResetMessage(true);
        return true;
    }

    @Override
    protected boolean isChannelRestricted() {
        return false;
    }
}
