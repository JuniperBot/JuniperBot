package ru.caramel.juniperbot.audio.service;

import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.exceptions.ErrorResponseException;
import net.dv8tion.jda.core.exceptions.PermissionException;
import net.dv8tion.jda.core.requests.RestAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import ru.caramel.juniperbot.audio.model.TrackRequest;
import ru.caramel.juniperbot.configuration.DiscordConfig;

import java.awt.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Function;

import static org.apache.commons.lang3.time.DurationFormatUtils.formatDuration;

@Service
public class MessageManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageManager.class);

    @Autowired
    private DiscordConfig discordConfig;

    @Autowired
    private TaskScheduler scheduler;

    public void onTrackAdd(TrackRequest request, boolean silent) {
        if (!silent) {
            sendMessageSilent(request.getChannel()::sendMessage, getBasicMessage(request).build());
        }
    }

    public void onTrackStart(TrackRequest request) {
        try {
            request.getChannel()
                    .sendMessage(getPlayMessage(request).build())
                    .queue(e -> {
                        request.setInfoMessage(e);
                        runUpdater(request);
                    });
        } catch (PermissionException e) {
            LOGGER.warn("No permission to message", e);
        }
    }

    public void onTrackEnd(TrackRequest request) {
        ScheduledFuture<?> task = request.getUpdaterTask();
        if (task != null) {
            task.cancel(false);
        }
        Message message = request.getInfoMessage();
        if (message != null) {
            try {
                message.delete().complete();
            } catch (PermissionException e) {
                LOGGER.warn("No permission to delete", e);
            } catch (ErrorResponseException e) {
                if (e.getErrorCode() != 10008 /* Unknown message */) {
                    throw e;
                }
            }
        }
    }

    public void onQueueEnd(TrackRequest request) {
        EmbedBuilder builder = getQueueMessage();
        builder.setDescription("Достигнут конец очереди воспроизведения :musical_note:");
        sendMessageSilent(request.getChannel()::sendMessage, builder.build());
    }

    public void onNoMatches(TextChannel sourceChannel, String query) {
        EmbedBuilder builder = getQueueMessage();
        builder.setDescription(String.format("По запросу **%s** ничего не найдено. :grey_question:", query));
        sendMessageSilent(sourceChannel::sendMessage, builder.build());
    }

    public void onError(TextChannel sourceChannel, FriendlyException e) {
        EmbedBuilder builder = getQueueMessage();
        builder.setColor(Color.RED);
        builder.setDescription(String.format("Произошла ошибка :interrobang:: %s", e.getMessage()));
        sendMessageSilent(sourceChannel::sendMessage, builder.build());
    }

    private void runUpdater(TrackRequest request) {
        if (discordConfig.getPlayRefreshInterval() != null) {
            ScheduledFuture<?> task = scheduler.scheduleWithFixedDelay(() -> {
                Message message = request.getInfoMessage();
                if (message != null) {
                    try {
                        message.editMessage(getPlayMessage(request).build()).complete();
                    } catch (Exception e) {
                        // fall down and skip
                    }
                }
            }, discordConfig.getPlayRefreshInterval());
            request.setUpdaterTask(task);
        }
    }

    private EmbedBuilder getQueueMessage() {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("Очередь воспроизведения", null);
        builder.setColor(discordConfig.getAccentColor());
        return builder;
    }

    private EmbedBuilder getPlayMessage(TrackRequest request) {
        EmbedBuilder builder = getBasicMessage(request);
        builder.setDescription(null);
        builder.addField("Длительность", getTextProgress(request.getTrack()), true);
        builder.addField("Поставил", request.getUser().getName(), true);
        return builder;
    }

    private EmbedBuilder getBasicMessage(TrackRequest request) {
        AudioTrackInfo info = request.getTrack().getInfo();
        String thumbUrl = getThumbnail(info);

        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle(info.title, info.uri);
        builder.setAuthor(info.author, info.uri, thumbUrl);
        builder.setThumbnail(thumbUrl);
        builder.setColor(discordConfig.getAccentColor());
        builder.setDescription("Добавлено в очередь воспроизведения :musical_note:");

        return builder;
    }

    private String getTextProgress(AudioTrack track) {
        return String.format("%s/%s", formatDuration(track.getPosition(), "HH:mm:ss"),
                formatDuration(track.getDuration(), "HH:mm:ss"));
    }

    private String getThumbnail(AudioTrackInfo info) {
        try {
            URI uri = new URI(info.uri);
            if (uri.getHost().contains("youtube.com") || uri.getHost().contains("youtu.be")) {
                return String.format("https://img.youtube.com/vi/%s/0.jpg", info.identifier);
            }
        } catch (URISyntaxException e) {
            // fall down
        }
        return null;
    }

    private void sendMessageSilent(Function<MessageEmbed, RestAction<Message>> action, MessageEmbed embed) {
        try {
            action.apply(embed).queue();
        } catch (PermissionException e) {
            LOGGER.warn("No permission to message", e);
        }
    }
}
