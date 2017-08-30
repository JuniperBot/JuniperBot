package ru.caramel.juniperbot.commands.audio.timing;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import ru.caramel.juniperbot.audio.model.TrackRequest;
import ru.caramel.juniperbot.commands.audio.AudioCommand;
import ru.caramel.juniperbot.commands.model.BotContext;
import ru.caramel.juniperbot.integration.discord.model.DiscordException;
import ru.caramel.juniperbot.utils.CommonUtils;

public abstract class TimingCommand extends AudioCommand {

    protected abstract boolean doInternal(MessageReceivedEvent message, TrackRequest request, long millis);

    @Override
    protected boolean doInternal(MessageReceivedEvent message, BotContext context, String content) throws DiscordException {
        TrackRequest current = playerService.getInstance(message.getGuild()).getCurrent();
        if (current == null) {
            messageManager.onMessage(message.getChannel(), "Воспроизведение не запущено");
            return false;
        }
        if (!current.getTrack().isSeekable()) {
            messageManager.onQueueError(message.getChannel(), "Текущую композицию невозможно перематывать");
            return false;
        }

        Long millis = CommonUtils.parseMillis(content);
        if (millis == null) {
            messageManager.onQueueError(message.getChannel(), "Введите корректный формат времени [[чч:]мм:]сс :stopwatch:");
            return false;
        }
        return doInternal(message, current, millis);
    }
}
