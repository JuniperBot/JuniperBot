package ru.caramel.juniperbot.commands.phyr;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.PermissionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import ru.caramel.juniperbot.commands.model.BotContext;
import ru.caramel.juniperbot.commands.model.CommandSource;
import ru.caramel.juniperbot.commands.model.DiscordCommand;
import ru.caramel.juniperbot.integration.discord.model.DiscordException;
import ru.caramel.juniperbot.service.PostService;

@DiscordCommand(key = "нафыркивай", description = "Автоматически нафыркивать новые посты из блога Джупи", source = CommandSource.GUILD)
public class AutoPostCommand extends PostCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutoPostCommand.class);

    @Autowired
    private PostService postService;

    @Override
    public boolean doCommand(MessageReceivedEvent message, BotContext context) throws DiscordException {
        try {
            boolean subscribed = postService.switchSubscription(message.getTextChannel());
            message.getChannel().sendMessage(subscribed
                    ? "Хорошо! Как только будет что-то новенькое я сюда фыркну ^_^"
                    : "Ты меня уже просил нафыркивать! Может ты хотел чтобы я перестала? Так тому и быть! *обиделась*").queue();
            return true;
        } catch (PermissionException e) {
            LOGGER.warn("No permissions to message", e);
        }
        return false;
    }
}
