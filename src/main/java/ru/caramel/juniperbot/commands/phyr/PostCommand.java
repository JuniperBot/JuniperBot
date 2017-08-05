package ru.caramel.juniperbot.commands.phyr;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.apache.commons.lang3.StringUtils;
import org.jinstagram.entity.users.feed.MediaFeedData;
import org.jinstagram.exceptions.InstagramException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import ru.caramel.juniperbot.commands.Command;
import ru.caramel.juniperbot.commands.DiscordCommand;
import ru.caramel.juniperbot.integration.discord.DiscordClient;
import ru.caramel.juniperbot.integration.instagram.InstagramClient;
import ru.caramel.juniperbot.model.BotContext;
import ru.caramel.juniperbot.model.exception.DiscordException;
import ru.caramel.juniperbot.model.exception.ValidationException;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

@DiscordCommand(key = "фыр", description = "Фыркнуть посты из блога Джупи (можно указать количество постов, по-умолчанию одно)")
public class PostCommand implements Command {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostCommand.class);

    public static final int MAX_DETAILED = 3;

    @Autowired
    private InstagramClient instagramClient;

    @Override
    public boolean doCommand(MessageReceivedEvent message, BotContext context, String[] args) throws DiscordException {
        int count = parseCount(context, args);
        List<MediaFeedData> medias = null;
        try {
            medias = instagramClient.getRecent();
        } catch (InstagramException e) {
            LOGGER.error("Could not get instagram data", e);
        }

        if (medias == null) {
            message.getChannel().sendMessage("Произошла какая-то ошибка у моего блога... Давай попробуем позже?").submit();
            return false;
        }
        if (medias.isEmpty()) {
            message.getChannel().sendMessage("Что-то мне и нечего показать...").submit();
            return false;
        }

        if (count > medias.size()) {
            message.getChannel().sendMessage(String.format("У меня есть всего %s сообщений...", medias.size())).submit();
            count = medias.size();
        }
        medias = medias.subList(0, count);
        post(medias, context);
        return true;
    }

    protected void post(List<MediaFeedData> medias, BotContext context) {
        if (medias.size() > 0) {
            if (context.isDetailedEmbed()) {
                for (int i = 0; i < Math.min(MAX_DETAILED, medias.size()); i++) {
                    MessageEmbed embed = convertToEmbed(context, medias.get(i));
                    context.getChannel().sendMessage(embed).submit();
                }
            } else {
                Iterator<MediaFeedData> iterator = medias.iterator();
                List<String> messages = new ArrayList<>();
                StringBuilder builder = new StringBuilder();
                while (iterator.hasNext()) {
                    MediaFeedData media = iterator.next();
                    String newEntry = media.getImages().getStandardResolution().getImageUrl() + '\n';
                    if (builder.length() + newEntry.length() > DiscordClient.MAX_MESSAGE_SIZE) {
                        messages.add(builder.toString());
                        builder = new StringBuilder();
                    }
                    builder.append(newEntry);
                }
                if (builder.length() > 0) {
                    messages.add(builder.toString());
                }
                for (String part : messages) {
                    context.getChannel().sendMessage(part).submit();
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        LOGGER.error("Error", e);
                    }
                }
            }
        }
    }

    private static int parseCount(BotContext context, String[] args) throws ValidationException {
        int count = 1;
        if (args.length > 0) {
            try {
                count = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                throw new ValidationException("Фыр на тебя. Число мне, число!");
            }

            if (count == 0) {
                throw new ValidationException("Всмысле ноль? Ну ладно, не буду ничего присылать.");
            }

            if (context.isDetailedEmbed()) {
                if (count > MAX_DETAILED) {
                    throw new ValidationException("Не могу прислать больше 3 фырок в нефырном виде :C");
                }
            } else if (count > 20) {
                throw new ValidationException("Не могу прислать больше 20 фырок за раз :C");
            }
            if (count < 0) {
                throw new ValidationException("Фтооо ты хочешь от меня?");
            }
        }
        return count;
    }

    public static MessageEmbed convertToEmbed(BotContext context, MediaFeedData media) {
        EmbedBuilder builder = new EmbedBuilder().setImage(media.getImages().getStandardResolution().getImageUrl());

        if (context == null || context.isDetailedEmbed()) {
            builder.setAuthor(media.getUser().getFullName(), null, media.getUser().getProfilePictureUrl());
            builder.setTimestamp(new Date(Long.parseLong(media.getCreatedTime()) * 1000).toInstant());

            if (media.getCaption() != null) {
                String text = media.getCaption().getText();
                if (StringUtils.isNotEmpty(text)) {
                    if (text.length() > 2000) {
                        text = text.substring(0, 1999);
                    }
                    if (media.getCaption().getText().length() > 200) {
                        builder.setTitle(media.getLink(), media.getLink());
                        builder.setDescription(text);
                    } else {
                        builder.setTitle(text, media.getLink());
                    }
                }
            }
        }
        return builder.build();
    }

}
