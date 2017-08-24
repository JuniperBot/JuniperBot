package ru.caramel.juniperbot.commands.phyr;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.PermissionException;
import org.apache.commons.lang3.StringUtils;
import org.jinstagram.entity.users.feed.MediaFeedData;
import org.jinstagram.exceptions.InstagramException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import ru.caramel.juniperbot.commands.Command;
import ru.caramel.juniperbot.commands.model.DiscordCommand;
import ru.caramel.juniperbot.configuration.DiscordConfig;
import ru.caramel.juniperbot.integration.instagram.InstagramClient;
import ru.caramel.juniperbot.commands.model.BotContext;
import ru.caramel.juniperbot.integration.discord.model.DiscordException;
import ru.caramel.juniperbot.commands.model.ValidationException;
import ru.caramel.juniperbot.service.PostService;

import java.util.List;

@DiscordCommand(key = "фыр", description = "Фыркнуть посты из блога Джупи (можно указать количество постов, по-умолчанию одно)")
public class PostCommand implements Command {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostCommand.class);

    @Autowired
    private InstagramClient instagramClient;

    @Autowired
    protected PostService postService;

    @Override
    public boolean doCommand(MessageReceivedEvent message, BotContext context, String content) throws DiscordException {
        int count = parseCount(content);
        List<MediaFeedData> medias = null;
        try {
            medias = instagramClient.getRecent();
        } catch (InstagramException e) {
            LOGGER.error("Could not get instagram data", e);
        }

        try {
            if (medias == null) {
                message.getChannel().sendMessage("Произошла какая-то ошибка у моего блога... Давай попробуем позже?").queue();
                return false;
            }
            if (medias.isEmpty()) {
                message.getChannel().sendMessage("Что-то мне и нечего показать...").queue();
                return false;
            }

            if (count > medias.size()) {
                message.getChannel().sendMessage(String.format("У меня есть всего %s сообщений...", medias.size())).queue();
                count = medias.size();
            }
            medias = medias.subList(0, count);
            postService.post(medias, message.getChannel());
        } catch (PermissionException e) {
            LOGGER.warn("No permissions to message", e);
        }
        return true;
    }

    private static int parseCount(String content) throws ValidationException {
        int count = 1;
        if (StringUtils.isNotEmpty(content)) {
            try {
                count = Integer.parseInt(content);
            } catch (NumberFormatException e) {
                throw new ValidationException("Фыр на тебя. Число мне, число!");
            }
            if (count == 0) {
                throw new ValidationException("Всмысле ноль? Ну ладно, не буду ничего присылать.");
            } else if (count > DiscordConfig.MAX_DETAILED) {
                throw new ValidationException("Не могу прислать больше 3 фырок :C");
            } else if (count < 0) {
                throw new ValidationException("Фтооо ты хочешь от меня?");
            }
        }
        return count;
    }
}
