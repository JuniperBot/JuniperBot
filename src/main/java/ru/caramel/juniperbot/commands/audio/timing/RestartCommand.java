package ru.caramel.juniperbot.commands.audio.timing;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import ru.caramel.juniperbot.commands.audio.AudioCommand;
import ru.caramel.juniperbot.commands.model.BotContext;
import ru.caramel.juniperbot.commands.model.CommandGroup;
import ru.caramel.juniperbot.commands.model.CommandSource;
import ru.caramel.juniperbot.commands.model.DiscordCommand;
import ru.caramel.juniperbot.integration.discord.model.DiscordException;

@DiscordCommand(
        key = "сначала",
        description = "Начать воспроизведение текущей композиции с начала",
        source = CommandSource.GUILD,
        group = CommandGroup.MUSIC,
        priority = 112)
public class RestartCommand extends AudioCommand {
    @Override
    protected boolean doInternal(MessageReceivedEvent message, BotContext context, String content) throws DiscordException {
        if (!playerService.getInstance(message.getGuild()).seek(0)) {
            messageManager.onMessage(message.getChannel(), "discord.command.audio.restart.denied");
            return false;
        }
        return true;
    }
}
