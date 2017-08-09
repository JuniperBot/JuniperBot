package ru.caramel.juniperbot.commands.phyr;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.PermissionException;
import org.jinstagram.entity.users.feed.MediaFeedData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.caramel.juniperbot.commands.DiscordCommand;
import ru.caramel.juniperbot.integration.instagram.InstagramListener;
import ru.caramel.juniperbot.model.BotContext;
import ru.caramel.juniperbot.model.CommandSource;
import ru.caramel.juniperbot.model.exception.DiscordException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@DiscordCommand(key = "нафыркивай", description = "Автоматически нафыркивать новые посты из блога Джупи", source = CommandSource.GUILD)
public class AutoPostCommand extends PostCommand implements InstagramListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutoPostCommand.class);

    private Set<BotContext> subscriptions = Collections.newSetFromMap(new ConcurrentHashMap<>());

    @Override
    public boolean doCommand(MessageReceivedEvent message, BotContext context) throws DiscordException {
        try {
            message.getChannel().sendMessage(
                    subscriptions.add(context)
                            ? "Хорошо! Как только будет что-то новенькое я сюда фыркну ^_^"
                            : "Ты меня уже просил нафыркивать!").queue();
            context.getSubscriptions().add(message.getChannel());
            return true;
        } catch (PermissionException e) {
            LOGGER.warn("No permissions to message", e);
        }
        return false;
    }

    @Override
    public void onInstagramUpdated(List<MediaFeedData> medias) {
        try {
            subscriptions.forEach(context -> {
                if (context.getLatestId() != null) {
                    List<MediaFeedData> newMedias = new ArrayList<>();
                    for (MediaFeedData media : medias) {
                        if (media.getId().equals(context.getLatestId())) {
                            break;
                        }
                        newMedias.add(media);
                    }

                    if (!newMedias.isEmpty() && !context.getSubscriptions().isEmpty()) {
                        context.getSubscriptions().forEach(channel -> post(newMedias, channel));
                    }
                }
                context.setLatestId(medias.get(0).getId());
            });
        } catch (PermissionException e) {
            LOGGER.warn("No permissions to message", e);
        }
    }
}
