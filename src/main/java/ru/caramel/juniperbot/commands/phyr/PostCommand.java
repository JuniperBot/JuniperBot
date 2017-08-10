package ru.caramel.juniperbot.commands.phyr;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.PermissionException;
import org.apache.commons.lang3.StringUtils;
import org.jinstagram.entity.users.feed.MediaFeedData;
import org.jinstagram.exceptions.InstagramException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import ru.caramel.juniperbot.commands.DiscordCommand;
import ru.caramel.juniperbot.commands.ParameterizedCommand;
import ru.caramel.juniperbot.configuration.DiscordConfig;
import ru.caramel.juniperbot.integration.instagram.InstagramClient;
import ru.caramel.juniperbot.model.BotContext;
import ru.caramel.juniperbot.model.exception.DiscordException;
import ru.caramel.juniperbot.model.exception.ValidationException;

import java.util.Date;
import java.util.List;

@DiscordCommand(key = "фыр", description = "Фыркнуть посты из блога Джупи (можно указать количество постов, по-умолчанию одно)")
public class PostCommand extends ParameterizedCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostCommand.class);

    public static final int MAX_DETAILED = 3;

    @Autowired
    private InstagramClient instagramClient;

    @Autowired
    private DiscordConfig discordConfig;

    @Override
    public boolean doCommand(MessageReceivedEvent message, BotContext context, String[] args) throws DiscordException {
        int count = parseCount(args);
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
            post(medias, message.getChannel());
        } catch (PermissionException e) {
            LOGGER.warn("No permissions to message", e);
        }
        return true;
    }

    protected void post(List<MediaFeedData> medias, MessageChannel channel) {
        if (medias.size() > 0) {
            for (int i = 0; i < Math.min(MAX_DETAILED, medias.size()); i++) {
                EmbedBuilder builder = convertToEmbed(medias.get(i));
                builder.setColor(discordConfig.getAccentColor());
                channel.sendMessage(builder.build()).queue();
            }
        }
    }

    private static int parseCount(String[] args) throws ValidationException {
        int count = 1;
        if (args.length > 0) {
            try {
                count = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                throw new ValidationException("Фыр на тебя. Число мне, число!");
            }
            if (count == 0) {
                throw new ValidationException("Всмысле ноль? Ну ладно, не буду ничего присылать.");
            } else if (count > MAX_DETAILED) {
                throw new ValidationException("Не могу прислать больше 3 фырок :C");
            } else if (count < 0) {
                throw new ValidationException("Фтооо ты хочешь от меня?");
            }
        }
        return count;
    }

    public static EmbedBuilder convertToEmbed(MediaFeedData media) {
        EmbedBuilder builder = new EmbedBuilder().setImage(media.getImages().getStandardResolution().getImageUrl());
        builder.setAuthor(media.getUser().getFullName(), null, media.getUser().getProfilePictureUrl());
        builder.setTimestamp(new Date(Long.parseLong(media.getCreatedTime()) * 1000).toInstant());

        if (media.getCaption() != null) {
            String text = media.getCaption().getText();
            if (StringUtils.isNotEmpty(text)) {
                if (text.length() > MessageEmbed.EMBED_MAX_LENGTH_CLIENT) {
                    text = text.substring(0, MessageEmbed.EMBED_MAX_LENGTH_CLIENT - 1);
                }
                if (media.getCaption().getText().length() > 200) {
                    builder.setTitle(media.getLink(), media.getLink());
                    builder.setDescription(text);
                } else {
                    builder.setTitle(text, media.getLink());
                }
            }
        }
        return builder;
    }

}
