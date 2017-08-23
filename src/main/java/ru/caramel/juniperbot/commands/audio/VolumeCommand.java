package ru.caramel.juniperbot.commands.audio;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import ru.caramel.juniperbot.audio.service.MessageManager;
import ru.caramel.juniperbot.audio.service.PlaybackManager;
import ru.caramel.juniperbot.commands.base.ParameterizedCommand;
import ru.caramel.juniperbot.commands.model.*;
import ru.caramel.juniperbot.integration.discord.model.DiscordException;

@DiscordCommand(
        key = "громкость",
        description = "Установить громкость воспроизведения (параметр 10-100%, без параметра 100%)",
        source = CommandSource.GUILD,
        group = CommandGroup.MUSIC)
public class VolumeCommand extends ParameterizedCommand {

    @Autowired
    private PlaybackManager playbackManager;

    @Autowired
    private MessageManager messageManager;

    @Override
    protected boolean doCommand(MessageReceivedEvent message, BotContext context, String[] args) throws DiscordException {
        int volume = parseCount(args);
        playbackManager.setVolume(message.getGuild(), volume);
        messageManager.onMessage(message.getChannel(), String.format("Громкость установлена на %d%% %s", volume,
                volume > 50 ? ":loud_sound:" : ":sound:"));
        return true;
    }

    private static int parseCount(String[] args) throws ValidationException {
        int count = 100;
        if (args.length > 0) {
            try {
                count = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                throw new ValidationException("Фыр на тебя. Число мне, число!");
            }
            if (count < 10) {
                throw new ValidationException("10% это слишком тихо...");
            } else if (count > 100) {
                throw new ValidationException("Максимальная громкость 100%!");
            } else if (count < 0) {
                throw new ValidationException("Фтооо ты хочешь от меня?");
            }
        }
        return count;
    }

}
