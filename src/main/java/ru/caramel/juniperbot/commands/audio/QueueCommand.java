package ru.caramel.juniperbot.commands.audio;

import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import ru.caramel.juniperbot.audio.model.TrackRequest;
import ru.caramel.juniperbot.audio.service.MessageManager;
import ru.caramel.juniperbot.audio.service.PlaybackManager;
import ru.caramel.juniperbot.commands.base.AbstractCommand;
import ru.caramel.juniperbot.commands.model.CommandGroup;
import ru.caramel.juniperbot.commands.model.DiscordCommand;
import ru.caramel.juniperbot.commands.model.BotContext;
import ru.caramel.juniperbot.commands.model.CommandSource;
import ru.caramel.juniperbot.integration.discord.model.DiscordException;

import java.util.List;

@DiscordCommand(
        key = "очередь",
        description = "Показать очередь воспроизведения",
        source = CommandSource.GUILD,
        group = CommandGroup.MUSIC)
public class QueueCommand extends AbstractCommand {

    @Autowired
    private PlaybackManager playbackManager;

    @Autowired
    private MessageManager messageManager;

    @Override
    public boolean doCommand(MessageReceivedEvent message, BotContext context, String content) throws DiscordException {
        int pageNum = 1;
        if (StringUtils.isNotEmpty(content)) {
            try {
                pageNum = Integer.parseInt(content);
            } catch (Exception e) {
                pageNum = 0;
            }
        }
        if (pageNum < 1) {
            messageManager.onError(message.getTextChannel(), "Укажите корректный номер страницы");
            return false;
        }
        return print(message.getTextChannel(), playbackManager.getQueue(message.getGuild()), pageNum);
    }

    private boolean print(TextChannel channel, List<TrackRequest> requests, int pageNum) {
        if (requests.isEmpty()) {
            messageManager.onEmptyQueue(channel);
            return true;
        }

        messageManager.onQueue(channel, requests, pageNum);
        return true;
    }
}
