package ru.caramel.juniperbot.audio.service;

import com.google.common.collect.Lists;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.exceptions.ErrorResponseException;
import net.dv8tion.jda.core.exceptions.PermissionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import ru.caramel.juniperbot.audio.model.TrackRequest;
import ru.caramel.juniperbot.commands.model.BotContext;
import ru.caramel.juniperbot.configuration.DiscordConfig;
import ru.caramel.juniperbot.service.MessageService;
import ru.caramel.juniperbot.utils.CommonUtils;

import java.awt.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

@Service
public class AudioMessageManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(AudioMessageManager.class);

    @Autowired
    private DiscordConfig discordConfig;

    @Autowired
    private TaskScheduler scheduler;

    @Autowired
    private MessageService messageService;

    public void onTrackAdd(TrackRequest request, boolean silent) {
        if (!silent) {
            messageService.sendMessageSilent(request.getChannel()::sendMessage, getBasicMessage(request).build());
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
        deleteMessage(request);
    }

    private void deleteMessage(TrackRequest request) {
        Message message = request.getInfoMessage();
        if (message != null) {
            try {
                message.delete().complete();
                request.setInfoMessage(null);
            } catch (PermissionException e) {
                LOGGER.warn("No permission to delete", e);
            } catch (ErrorResponseException e) {
                if (e.getErrorCode() != 10008 /* Unknown message */) {
                    throw e;
                }
            }
        }
    }

    public void onTrackPause(TrackRequest request) {
        ScheduledFuture<?> task = request.getUpdaterTask();
        if (task != null) {
            task.cancel(false);
        }
    }

    public void onTrackResume(TrackRequest request) {
        deleteMessage(request);
        onTrackStart(request);
    }

    public void onQueueEnd(TrackRequest request) {
        EmbedBuilder builder = getQueueMessage();
        builder.setDescription("Достигнут конец очереди воспроизведения :musical_note:");
        messageService.sendMessageSilent(request.getChannel()::sendMessage, builder.build());
    }

    public void onMessage(MessageChannel sourceChannel, String message) {
        EmbedBuilder builder = getQueueMessage();
        builder.setDescription(message);
        messageService.sendMessageSilent(sourceChannel::sendMessage, builder.build());
    }

    public void onNoMatches(MessageChannel sourceChannel, String query) {
        EmbedBuilder builder = getQueueMessage();
        builder.setDescription(String.format("По запросу **%s** ничего не найдено. :grey_question:", query));
        messageService.sendMessageSilent(sourceChannel::sendMessage, builder.build());
    }

    public void onQueueError(MessageChannel sourceChannel, String error) {
        EmbedBuilder builder = getQueueMessage();
        builder.setColor(Color.RED);
        builder.setDescription(error);
        messageService.sendMessageSilent(sourceChannel::sendMessage, builder.build());
    }

    public void onEmptyQueue(MessageChannel sourceChannel) {
        EmbedBuilder builder = getQueueMessage();
        builder.setColor(Color.RED);
        builder.setDescription("Очередь воспроизведения пуста :flag_white: ");
        messageService.sendMessageSilent(sourceChannel::sendMessage, builder.build());
    }

    public void onTimeout(MessageChannel sourceChannel) {
        EmbedBuilder builder = getQueueMessage();
        builder.setDescription("Воспроизведение остановлено, поскольку не осталось слушателей");
        messageService.sendMessageSilent(sourceChannel::sendMessage, builder.build());
    }

    public void onQueue(MessageChannel sourceChannel, BotContext context, List<TrackRequest> requests, int pageNum) {
        final int pageSize = 25;
        List<List<TrackRequest>> parts = Lists.partition(requests, pageSize);
        final int totalPages = parts.size();
        final int offset = (pageNum - 1) * pageSize + 1;

        if (pageNum > totalPages) {
            onQueueError(sourceChannel, String.format("Всего страниц: %d", parts.size()));
            return;
        }
        List<TrackRequest> pageRequests = parts.get(pageNum - 1);

        EmbedBuilder builder = getQueueMessage();
        for (int i = 0; i < pageRequests.size(); i++) {
            TrackRequest request = pageRequests.get(i);
            AudioTrack track = request.getTrack();
            AudioTrackInfo info = track.getInfo();

            int rowNum = i + offset;
            String title = String.format("%d. %s [%s](%s) от %s", rowNum, rowNum == 1 ? ":musical_note: " : "",
                    info.title, info.uri, request.getUser().getName());
            builder.addField(EmbedBuilder.ZERO_WIDTH_SPACE, title, false);
        }
        if (totalPages > 1) {
            builder.setFooter(String.format("Страница %d из %d, всего %d в очереди. Введите: %sочередь <номер>",
                    pageNum, totalPages, requests.size(), context.getPrefix()), null);
        }

        messageService.sendMessageSilent(sourceChannel::sendMessage, builder.build());
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
        return messageService.getBaseEmbed().setTitle("Очередь воспроизведения", null);
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

        EmbedBuilder builder = messageService.getBaseEmbed();
        builder.setTitle(info.title, info.uri);
        builder.setAuthor(info.author, info.uri, thumbUrl);
        builder.setThumbnail(thumbUrl);
        builder.setDescription("Добавлено в очередь воспроизведения :musical_note:");

        return builder;
    }

    private String getTextProgress(AudioTrack track) {
        StringBuilder builder = new StringBuilder(CommonUtils.formatDuration(track.getPosition()));
        if (!track.getInfo().isStream) {
            builder.append("/").append(CommonUtils.formatDuration(track.getDuration()));
        } else {
            builder.append(" (поток)");
        }
        return builder.toString();
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
}
