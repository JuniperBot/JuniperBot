package ru.caramel.juniperbot.commands.audio.queue;

import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.apache.commons.lang3.StringUtils;
import ru.caramel.juniperbot.audio.model.TrackRequest;
import ru.caramel.juniperbot.commands.audio.AudioCommand;
import ru.caramel.juniperbot.commands.model.CommandGroup;
import ru.caramel.juniperbot.commands.model.DiscordCommand;
import ru.caramel.juniperbot.commands.model.BotContext;
import ru.caramel.juniperbot.commands.model.CommandSource;
import ru.caramel.juniperbot.integration.discord.model.DiscordException;

import java.util.List;

@DiscordCommand(
        key = "discord.command.queue.key",
        description = "discord.command.queue.desc",
        source = CommandSource.GUILD,
        group = CommandGroup.MUSIC,
        priority = 101)
public class QueueCommand extends AudioCommand {

    @Override
    public boolean doInternal(MessageReceivedEvent message, BotContext context, String content) throws DiscordException {
        int pageNum = 1;
        if (StringUtils.isNotEmpty(content)) {
            try {
                pageNum = Integer.parseInt(content);
            } catch (Exception e) {
                pageNum = 0;
            }
        }
        if (pageNum < 1) {
            messageManager.onQueueError(message.getTextChannel(), "discord.command.audio.queue.list.selectPage");
            return false;
        }
        return print(message.getTextChannel(), context, playerService.getInstance(message.getGuild()).getQueue(), pageNum);
    }

    private boolean print(TextChannel channel, BotContext context, List<TrackRequest> requests, int pageNum) {
        if (requests.isEmpty()) {
            messageManager.onEmptyQueue(channel);
            return true;
        }

        messageManager.onQueue(channel, context, requests, pageNum);
        return true;
    }

    @Override
    protected boolean isChannelRestricted() {
        return false;
    }
}
