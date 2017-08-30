package ru.caramel.juniperbot.commands.audio;

import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import ru.caramel.juniperbot.audio.service.PlayerService;
import ru.caramel.juniperbot.audio.service.AudioMessageManager;
import ru.caramel.juniperbot.commands.Command;
import ru.caramel.juniperbot.commands.model.BotContext;
import ru.caramel.juniperbot.commands.model.ValidationException;
import ru.caramel.juniperbot.integration.discord.model.DiscordException;

public abstract class AudioCommand implements Command {

    @Autowired
    protected PlayerService playerService;

    @Autowired
    protected AudioMessageManager messageManager;

    protected abstract boolean doInternal(MessageReceivedEvent message, BotContext context, String content) throws DiscordException;

    @Override
    public boolean doCommand(MessageReceivedEvent message, BotContext context, String content) throws DiscordException {
        if (isChannelRestricted() && !playerService.isInChannel(message.getMember())) {
            VoiceChannel channel = playerService.getChannel(message.getMember());
            throw new ValidationException(String.format("Сперва вы должны зайти в голосовой канал (%s) :raised_hand:", channel.getName()));
        }
        doInternal(message, context, content);
        return false;
    }

    protected boolean isChannelRestricted() {
        return true;
    }
}
