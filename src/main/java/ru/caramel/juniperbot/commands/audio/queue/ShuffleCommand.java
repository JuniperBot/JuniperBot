package ru.caramel.juniperbot.commands.audio.queue;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import ru.caramel.juniperbot.commands.audio.AudioCommand;
import ru.caramel.juniperbot.commands.model.BotContext;
import ru.caramel.juniperbot.commands.model.CommandGroup;
import ru.caramel.juniperbot.commands.model.CommandSource;
import ru.caramel.juniperbot.commands.model.DiscordCommand;
import ru.caramel.juniperbot.integration.discord.model.DiscordException;

@DiscordCommand(
        key = "перемешать",
        description = "Перемешать очередь воспроизведения",
        source = CommandSource.GUILD,
        group = CommandGroup.MUSIC,
        priority = 109)
public class ShuffleCommand extends AudioCommand {

    @Override
    public boolean doInternal(MessageReceivedEvent message, BotContext context, String content) throws DiscordException {
        if (playerService.getInstance(message.getGuild()).shuffle()) {
            messageManager.onMessage(message.getChannel(), "discord.command.audio.queue.shuffle");
        } else {
            messageManager.onEmptyQueue(message.getChannel());
        }
        return true;
    }
}
