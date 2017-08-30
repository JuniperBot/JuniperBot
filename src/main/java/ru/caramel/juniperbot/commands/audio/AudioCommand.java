package ru.caramel.juniperbot.commands.audio;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import ru.caramel.juniperbot.audio.service.PlaybackHandlerService;
import ru.caramel.juniperbot.audio.service.AudioMessageManager;
import ru.caramel.juniperbot.commands.Command;
import ru.caramel.juniperbot.commands.model.BotContext;
import ru.caramel.juniperbot.commands.model.ValidationException;
import ru.caramel.juniperbot.integration.discord.model.DiscordException;

public abstract class AudioCommand implements Command {

    @Autowired
    protected PlaybackHandlerService handlerService;

    @Autowired
    protected AudioMessageManager messageManager;

    protected abstract boolean doInternal(MessageReceivedEvent message, BotContext context, String content) throws DiscordException;

    @Override
    public boolean doCommand(MessageReceivedEvent message, BotContext context, String content) throws DiscordException {
        if (isChannelRestricted() && !handlerService.isInChannel(message.getMember())) {
            throw new ValidationException("Сперва вы должны зайти в голосовой канал :raised_hand:");
        }
        doInternal(message, context, content);
        return false;
    }

    protected boolean isChannelRestricted() {
        return true;
    }
}
