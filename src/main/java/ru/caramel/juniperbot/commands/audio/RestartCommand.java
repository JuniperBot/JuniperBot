package ru.caramel.juniperbot.commands.audio;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
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
        priority = 110)
public class RestartCommand extends AudioCommand {
    @Override
    protected boolean doInternal(MessageReceivedEvent message, BotContext context, String content) throws DiscordException {
        if (!handlerService.seek(message.getGuild(), 0)) {
            messageManager.onMessage(message.getChannel(), "Данную композицию нельзя воспроизвести с начала");
        }
        return false;
    }
}
