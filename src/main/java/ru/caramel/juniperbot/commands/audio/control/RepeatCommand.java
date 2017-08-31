package ru.caramel.juniperbot.commands.audio.control;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import ru.caramel.juniperbot.audio.model.RepeatMode;
import ru.caramel.juniperbot.commands.audio.AudioCommand;
import ru.caramel.juniperbot.commands.model.BotContext;
import ru.caramel.juniperbot.commands.model.CommandGroup;
import ru.caramel.juniperbot.commands.model.CommandSource;
import ru.caramel.juniperbot.commands.model.DiscordCommand;
import ru.caramel.juniperbot.integration.discord.model.DiscordException;

@DiscordCommand(
        key = "discord.command.repeat.key",
        description = "discord.command.repeat.desc",
        source = CommandSource.GUILD,
        group = CommandGroup.MUSIC,
        priority = 108)
public class RepeatCommand extends AudioCommand {
    @Override
    protected boolean doInternal(MessageReceivedEvent message, BotContext context, String content) throws DiscordException {
        RepeatMode mode = RepeatMode.getForTitle(content);
        if (mode == null) {
            messageManager.onMessage(message.getChannel(), "discord.command.audio.repeat.help", RepeatMode.options());
            return false;
        }
        if (playerService.getInstance(message.getGuild()).setMode(mode)) {
            messageManager.onMessage(message.getChannel(), "discord.command.audio.repeat", mode.getEmoji());
        } else {
            messageManager.onMessage(message.getChannel(), "discord.command.audio.notStarted");
        }
        return true;
    }
}
