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
import ru.caramel.juniperbot.audio.model.RepeatMode;
import ru.caramel.juniperbot.audio.model.TrackRequest;
import ru.caramel.juniperbot.commands.model.BotContext;
import ru.caramel.juniperbot.configuration.DiscordConfig;
import ru.caramel.juniperbot.service.MessageService;
import ru.caramel.juniperbot.utils.CommonUtils;

import java.awt.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
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
            request.setResetMessage(false);
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
                    info.title, info.uri, request.getMember().getEffectiveName());
            builder.addField(EmbedBuilder.ZERO_WIDTH_SPACE, title, false);
        }
        builder.setFooter(totalPages > 1
                ? messageService.getMessage("discord.command.audio.queue.list.pageFooter",
                pageNum, totalPages, requests.size(), CommonUtils.formatDuration(totalDuration), context.getPrefix())
                : messageService.getMessage("discord.command.audio.queue.list.footer",
                requests.size(), CommonUtils.formatDuration(totalDuration)), null);
        messageService.sendMessageSilent(sourceChannel::sendMessage, builder.build());
    }

    private void runUpdater(TrackRequest request) {
        if (discordConfig.getPlayRefreshInterval() != null) {
            ScheduledFuture<?> task = scheduler.scheduleWithFixedDelay(() -> {
                Message message;
                if (request.isResetMessage()) {
                    request.getInfoMessage().delete().queue();
                    message = request.getChannel()
                            .sendMessage(getPlayMessage(request).build())
                            .complete();
                    request.setInfoMessage(message);
                    request.setResetMessage(false);
                    return;
                }
                message = request.getInfoMessage();
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
        return messageService.getBaseEmbed().setTitle(messageService.getMessage("discord.command.audio.queue.title"), null);
    }

    private EmbedBuilder getPlayMessage(TrackRequest request) {
        EmbedBuilder builder = getBasicMessage(request);
        builder.setDescription(null);
        builder.addField(messageService.getMessage("discord.command.audio.panel.duration"),
                getTextProgress(request.getTrack()), true);
        builder.addField(messageService.getMessage("discord.command.audio.panel.requestedBy"),
                request.getMember().getEffectiveName(), true);

        PlaybackInstance handler = request.getTrack().getUserData(PlaybackInstance.class);
        if (handler != null) {
            if (handler.getPlayer().getVolume() < 100) {
                int volume = handler.getPlayer().getVolume();
                builder.addField(messageService.getMessage("discord.command.audio.panel.volume"),
                        String.format("%d%% %s", volume, CommonUtils.getVolumeIcon(volume)), true);
            }
            if (!RepeatMode.NONE.equals(handler.getMode())) {
                builder.addField(messageService.getMessage("discord.command.audio.panel.repeatMode"),
                        handler.getMode().getEmoji(), true);
            }
        }
        return builder;
    }

    private EmbedBuilder getBasicMessage(TrackRequest request) {
        AudioTrackInfo info = request.getTrack().getInfo();
        String thumbUrl = getThumbnail(info);

        EmbedBuilder builder = messageService.getBaseEmbed();
        builder.setTitle(info.title, info.uri);
        builder.setAuthor(info.author, info.uri, thumbUrl);
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
}
