/*
 * This file is part of JuniperBotJ.
 *
 * JuniperBotJ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * JuniperBotJ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with JuniperBotJ. If not, see <http://www.gnu.org/licenses/>.
 */
package ru.caramel.juniperbot.module.audio.service;

import com.google.common.collect.Lists;
import com.sedmelluq.discord.lavaplayer.container.MediaContainerDetection;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.exceptions.ErrorResponseException;
import net.dv8tion.jda.core.exceptions.PermissionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import ru.caramel.juniperbot.core.model.BotContext;
import ru.caramel.juniperbot.core.service.ContextService;
import ru.caramel.juniperbot.core.service.MessageService;
import ru.caramel.juniperbot.core.utils.CommonUtils;
import ru.caramel.juniperbot.module.audio.model.PlaybackInstance;
import ru.caramel.juniperbot.module.audio.model.RepeatMode;
import ru.caramel.juniperbot.module.audio.model.TrackRequest;
import ru.caramel.juniperbot.module.audio.utils.MessageController;

import java.awt.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Service
public class AudioMessageManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(AudioMessageManager.class);

    @Value("${audio.play.message.refreshInterval}")
    private Long playRefreshInterval;

    @Autowired
    private TaskScheduler scheduler;

    @Autowired
    private MessageService messageService;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private ContextService contextService;

    private Map<Guild, ScheduledFuture<?>> updaterTasks = new ConcurrentHashMap<>();

    private Map<Guild, MessageController> controllers = new ConcurrentHashMap<>();

    public void onTrackAdd(TrackRequest request, boolean silent) {
        if (!silent) {
            messageService.sendMessageSilent(request.getChannel()::sendMessage, getBasicMessage(request).build());
        }
    }

    public void onTrackStart(TrackRequest request) {
        synchronized (request.getGuild()) {
            try {
                request.setResetMessage(false);
                request.getChannel()
                        .sendMessage(getPlayMessage(request).build())
                        .queue(e -> {
                            MessageController oldController = controllers.put(request.getGuild(), new MessageController(context, e));
                            if (oldController != null) {
                                markAsPassed(request, oldController, true);
                            }
                            runUpdater(request);
                        });
            } catch (PermissionException e) {
                LOGGER.warn("No permission to message", e);
            }
        }
    }

    public void onTrackEnd(TrackRequest request) {
        synchronized (request.getGuild()) {
            cancelUpdate(request);
            controllers.computeIfPresent(request.getGuild(), (g, c) -> {
                markAsPassed(request, c, true);
                return null;
            });
        }
    }

    private void markAsPassed(TrackRequest request, MessageController controller, boolean soft) {
        try {
            controller.remove(soft);
            if (soft) {
                try {
                    controller.getMessage().editMessage(getPlayMessage(request, true).build()).complete();
                } catch (Exception e) {
                    // fall down and skip
                }
            }
        } catch (PermissionException e) {
            LOGGER.warn("No permission to delete", e);
        } catch (ErrorResponseException e) {
            if (e.getErrorCode() != 10008 /* Unknown message */) {
                throw e;
            }
        }
    }

    private void deleteMessage(TrackRequest request) {
        synchronized (request.getGuild()) {
            controllers.computeIfPresent(request.getGuild(), (g, c) -> {
                markAsPassed(request, c, false);
                return null;
            });
        }
    }

    private void cancelUpdate(TrackRequest request) {
        updaterTasks.computeIfPresent(request.getGuild(), (g, e) -> {
            e.cancel(false);
            return null;
        });
    }

    public void onTrackPause(TrackRequest request) {
        synchronized (request.getGuild()) {
            updateMessage(request);
            cancelUpdate(request);
        }
    }

    public void onTrackResume(TrackRequest request) {
        synchronized (request.getGuild()) {
            if (request.isResetOnResume()) {
                deleteMessage(request);
                onTrackStart(request);
            } else {
                runUpdater(request);
            }
        }
    }

    public void onQueueEnd(TrackRequest request) {
        EmbedBuilder builder = getQueueMessage();
        builder.setDescription(messageService.getMessage("discord.command.audio.queue.end"));
        messageService.sendMessageSilent(request.getChannel()::sendMessage, builder.build());
    }

    public void onMessage(MessageChannel sourceChannel, String code, Object... args) {
        EmbedBuilder builder = getQueueMessage();
        builder.setDescription(messageService.getMessage(code, args));
        messageService.sendMessageSilent(sourceChannel::sendMessage, builder.build());
    }

    public void onNoMatches(MessageChannel sourceChannel, String query) {
        EmbedBuilder builder = getQueueMessage();
        builder.setDescription(messageService.getMessage("discord.command.audio.search.noMatches", query));
        messageService.sendMessageSilent(sourceChannel::sendMessage, builder.build());
    }

    public void onQueueError(MessageChannel sourceChannel, String code, Object... args) {
        EmbedBuilder builder = getQueueMessage();
        builder.setColor(Color.RED);
        builder.setDescription(messageService.getMessage(code, args));
        messageService.sendMessageSilent(sourceChannel::sendMessage, builder.build());
    }

    public void onEmptyQueue(MessageChannel sourceChannel) {
        EmbedBuilder builder = getQueueMessage();
        builder.setColor(Color.RED);
        builder.setDescription(messageService.getMessage("discord.command.audio.queue.empty"));
        messageService.sendMessageSilent(sourceChannel::sendMessage, builder.build());
    }

    public void onIdle(MessageChannel sourceChannel) {
        EmbedBuilder builder = getQueueMessage();
        builder.setDescription(messageService.getMessage("discord.command.audio.queue.idle"));
        messageService.sendMessageSilent(sourceChannel::sendMessage, builder.build());
    }

    public void onQueue(MessageChannel sourceChannel, BotContext context, List<TrackRequest> requests, int pageNum) {
        final int pageSize = 25;
        List<List<TrackRequest>> parts = Lists.partition(requests, pageSize);
        final int totalPages = parts.size();
        final int offset = (pageNum - 1) * pageSize + 1;

        final long totalDuration = requests.stream()
                .filter(Objects::nonNull)
                .map(TrackRequest::getTrack)
                .filter(Objects::nonNull)
                .mapToLong(AudioTrack::getDuration).sum();

        if (pageNum > totalPages) {
            onQueueError(sourceChannel, "discord.command.audio.queue.list.totalPages", parts.size());
            return;
        }
        List<TrackRequest> pageRequests = parts.get(pageNum - 1);

        EmbedBuilder builder = getQueueMessage();
        for (int i = 0; i < pageRequests.size(); i++) {
            TrackRequest request = pageRequests.get(i);
            AudioTrack track = request.getTrack();
            AudioTrackInfo info = track.getInfo();

            int rowNum = i + offset;
            String title = messageService.getMessage("discord.command.audio.queue.list.entry", rowNum,
                    CommonUtils.formatDuration(track.getDuration()), rowNum == 1 ? ":musical_note: " : "",
                    getTitle(info), info.uri, request.getMember().getEffectiveName());
            builder.addField(EmbedBuilder.ZERO_WIDTH_SPACE, title, false);
        }
        builder.setFooter(totalPages > 1
                ? messageService.getMessage("discord.command.audio.queue.list.pageFooter",
                pageNum, totalPages, requests.size(), CommonUtils.formatDuration(totalDuration),
                context.getConfig().getPrefix())
                : messageService.getMessage("discord.command.audio.queue.list.footer",
                requests.size(), CommonUtils.formatDuration(totalDuration)), null);
        messageService.sendMessageSilent(sourceChannel::sendMessage, builder.build());
    }

    private void runUpdater(TrackRequest request) {
        synchronized (request.getGuild()) {
            if (playRefreshInterval != null) {
                ScheduledFuture<?> task = scheduler.scheduleWithFixedDelay(() ->
                                contextService.withContext(request.getGuild(), () -> updateMessage(request)),
                        playRefreshInterval);
                ScheduledFuture<?> oldTask = updaterTasks.put(request.getGuild(), task);
                if (oldTask != null) {
                    oldTask.cancel(true);
                }
            }
        }
    }

    public void updateMessage(TrackRequest request) {
        try {
            Message message;
            if (request.isResetMessage()) {
                message = request.getChannel()
                        .sendMessage(getPlayMessage(request).build())
                        .complete();
                MessageController oldController = controllers.put(request.getGuild(),
                        new MessageController(context, message));
                if (oldController != null) {
                    oldController.remove(false);
                }
                request.setResetMessage(false);
                return;
            }
            MessageController controller = controllers.get(request.getGuild());
            if (controller != null) {
                message = controller.getMessage();
                if (message != null) {
                    message.editMessage(getPlayMessage(request).build()).complete();
                }
            }
        } catch (PermissionException e) {
            LOGGER.warn("No permission to update", e);
            cancelUpdate(request);
        } catch (ErrorResponseException e) {
            if (e.getErrorCode() == 10008 /* Unknown message */) {
                cancelUpdate(request);
            } else {
                LOGGER.error("Update message error", e);
            }
        }
    }

    private EmbedBuilder getQueueMessage() {
        return messageService.getBaseEmbed().setTitle(messageService.getMessage("discord.command.audio.queue.title"), null);
    }

    private EmbedBuilder getPlayMessage(TrackRequest request, boolean passed) {
        EmbedBuilder builder = getBasicMessage(request);
        builder.setDescription(null);

        String durationText;
        if (passed) {
            durationText = request.getTrack().getInfo().isStream
                    ? messageService.getMessage("discord.command.audio.panel.duration.passedStream")
                    : messageService.getMessage("discord.command.audio.panel.duration.passed",
                    CommonUtils.formatDuration(request.getTrack().getDuration()));
        } else {
            durationText = getTextProgress(request.getTrack());
        }

        builder.addField(messageService.getMessage("discord.command.audio.panel.duration"),
                durationText, true);
        builder.addField(messageService.getMessage("discord.command.audio.panel.requestedBy"),
                request.getMember().getEffectiveName(), true);

        PlaybackInstance handler = request.getTrack().getUserData(PlaybackInstance.class);
        if (!passed && handler != null) {
            if (handler.getPlayer().getVolume() != 100) {
                int volume = handler.getPlayer().getVolume();
                builder.addField(messageService.getMessage("discord.command.audio.panel.volume"),
                        String.format("%d%% %s", volume, CommonUtils.getVolumeIcon(volume)), true);
            }
            if (!RepeatMode.NONE.equals(handler.getMode())) {
                builder.addField(messageService.getMessage("discord.command.audio.panel.repeatMode"),
                        handler.getMode().getEmoji(), true);
            }
            if (handler.getPlayer().isPaused()) {
                builder.addField(messageService.getMessage("discord.command.audio.panel.paused"),
                        "\u23F8", true);
            }
        }
        return builder;
    }

    private EmbedBuilder getPlayMessage(TrackRequest request) {
        return getPlayMessage(request, false);
    }

    private EmbedBuilder getBasicMessage(TrackRequest request) {
        AudioTrackInfo info = request.getTrack().getInfo();
        String thumbUrl = getThumbnail(info);

        EmbedBuilder builder = messageService.getBaseEmbed();
        builder.setTitle(getTitle(info), info.uri);
        builder.setAuthor(getArtist(info), info.uri, thumbUrl);
        builder.setThumbnail(thumbUrl);
        builder.setDescription(messageService.getMessage("discord.command.audio.queue.add"));
        return builder;
    }

    private String getTextProgress(AudioTrack track) {
        StringBuilder builder = new StringBuilder(CommonUtils.formatDuration(track.getPosition()));
        if (!track.getInfo().isStream) {
            builder.append("/").append(CommonUtils.formatDuration(track.getDuration()));
        } else {
            builder.append(" (")
                    .append(messageService.getMessage("discord.command.audio.panel.stream"))
                    .append(")");
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

    public String getTitle(AudioTrackInfo info) {
        return MediaContainerDetection.UNKNOWN_TITLE.equals(info.title)
                ? messageService.getMessage("discord.command.audio.panel.unknownTitle") : info.title;
    }

    public String getArtist(AudioTrackInfo info) {
        return MediaContainerDetection.UNKNOWN_ARTIST.equals(info.author)
                ? messageService.getMessage("discord.command.audio.panel.unknownArtist") : info.author;
    }
}
