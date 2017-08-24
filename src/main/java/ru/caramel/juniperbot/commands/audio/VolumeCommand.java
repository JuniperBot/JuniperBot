package ru.caramel.juniperbot.commands.audio;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import ru.caramel.juniperbot.commands.model.*;
import ru.caramel.juniperbot.integration.discord.model.DiscordException;

@DiscordCommand(
        key = "громкость",
        description = "Установить громкость воспроизведения (параметр 10-100%, без параметра 100%)",
        source = CommandSource.GUILD,
        group = CommandGroup.MUSIC)
public class VolumeCommand extends AudioCommand {

    @Override
    protected boolean doInternal(MessageReceivedEvent message, BotContext context, String content) throws DiscordException {
        int volume = parseCount(content);
        handlerService.setVolume(message.getGuild(), volume);
        messageManager.onMessage(message.getChannel(), String.format("Громкость установлена на %d%% %s", volume,
                volume > 50 ? ":loud_sound:" : ":sound:"));
        return true;
    }

    private static int parseCount(String content) throws ValidationException {
        int count = 100;
        if (!content.isEmpty()) {
            try {
                count = Integer.parseInt(content);
            } catch (NumberFormatException e) {
                throw new ValidationException("Фыр на тебя. Число мне, число!");
            }
            if (count < 0) {
                throw new ValidationException("Фтооо ты хочешь от меня?");
            } else if (count < 10) {
                throw new ValidationException("10% это слишком тихо...");
            } else if (count > 100) {
                throw new ValidationException("Максимальная громкость 100%!");
            }
        }
        return count;
    }
}
