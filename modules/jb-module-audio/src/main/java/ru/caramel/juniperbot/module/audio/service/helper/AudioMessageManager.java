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
package ru.caramel.juniperbot.module.audio.service.helper;

import com.google.common.collect.Lists;
import com.sedmelluq.discord.lavaplayer.container.MediaContainerDetection;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackState;
import net.dv8tion.jda.bot.sharding.ShardManager;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.exceptions.ErrorResponseException;
import net.dv8tion.jda.core.exceptions.PermissionException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import ru.caramel.juniperbot.core.model.BotContext;
import ru.caramel.juniperbot.core.service.*;
import ru.caramel.juniperbot.core.utils.CommonUtils;
import ru.caramel.juniperbot.module.audio.model.PlaybackInstance;
import ru.caramel.juniperbot.module.audio.model.RepeatMode;
import ru.caramel.juniperbot.module.audio.model.TrackRequest;
import ru.caramel.juniperbot.module.audio.utils.MessageController;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class AudioMessageManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(AudioMessageManager.class);

    @Value("${discord.audio.ui.refreshInterval:5000}")
    private Long playRefreshInterval;

    @Autowired
    private TaskScheduler scheduler;

    @Autowired
    private MessageService messageService;

    @Autowired
    private BrandingService brandingService;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private ContextService contextService;

    @Autowired
    private DiscordService discordService;

    @Autowired
    private FeatureSetService featureSetService;

    private Map<Long, ScheduledFuture<?>> updaterTasks = new ConcurrentHashMap<>();

    private Map<Long, MessageController> controllers = new ConcurrentHashMap<>();

    private Map<Long, ReentrantLock> guildLocks = new ConcurrentHashMap<>();

    public void onTrackAdd(TrackRequest request, boolean silent) {
        if (!silent) {
            TextChannel channel = request.getChannel();
            if (channel != null) {
                messageService.sendMessageSilent(channel::sendMessage, getBasicMessage(request).build());
            }
        }
    }

    public void onTrackStart(TrackRequest request) {
        if (request.getEndReason() != null || request.getTrack().getState() == AudioTrackState.FINISHED) {
            return;
        }
        TextChannel channel = request.getChannel();
        if (channel == null) {
            return;
        }
        syncByGuild(request, () -> {
            try {
                request.setResetMessage(false);
                channel.sendMessage(getPlayMessage(request).build())
                        .queue(e -> {
                            MessageController oldController = controllers.put(request.getGuildId(),
                                    new MessageController(context, e));
                            if (oldController != null) {
                                contextService.withContext(request.getGuildId(),
                                        () -> markAsPassed(request, oldController, true));
                            }
                            if (featureSetService.isAvailable(request.getGuildId())) {
                                runUpdater(request);
                            }
                        });
            } catch (PermissionException e) {
                LOGGER.warn("No permission to message", e);
            }
        });
    }

    public void onTrackEnd(TrackRequest request) {
        syncByGuild(request, () -> {
            cancelUpdate(request);
            controllers.computeIfPresent(request.getGuildId(), (g, c) -> {
                markAsPassed(request, c, true);
                return null;
            });
        });
    }

    private void markAsPassed(TrackRequest request, MessageController controller, boolean soft) {
        try {
            controller.remove(soft);
            if (soft) {
                try {
                    controller.doForMessage(message -> {
                        message.editMessage(getPlayMessage(request).build()).queue();
                    });
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
        syncByGuild(request, () -> {
            controllers.computeIfPresent(request.getGuildId(), (g, c) -> {
                markAsPassed(request, c, false);
                return null;
            });
        });
    }

    public void clear(long guildId) {
        cancelUpdate(guildId);
        controllers.remove(guildId);
    }

    public void cancelUpdate(TrackRequest request) {
        cancelUpdate(request.getGuildId());
    }

    public void cancelUpdate(long guildId) {
        updaterTasks.computeIfPresent(guildId, (g, e) -> {
            e.cancel(false);
            return null;
        });
    }

    public void onTrackPause(TrackRequest request) {
        syncByGuild(request, () -> {
            updateMessage(request);
            cancelUpdate(request);
        });
    }

    public void onTrackResume(TrackRequest request) {
        syncByGuild(request, () -> {
            if (request.isResetOnResume()) {
                deleteMessage(request);
                onTrackStart(request);
            } else {
                runUpdater(request);
            }
        });
    }

    public void onQueueEnd(TrackRequest request) {
        TextChannel channel = request.getChannel();
        if (channel == null) {
            return;
        }

        EmbedBuilder builder = getQueueMessage();
        builder.setDescription(messageService.getMessage("discord.command.audio.queue.end"));
        messageService.sendMessageSilent(channel::sendMessage, builder.build());
    }

    public void onMessage(MessageChannel sourceChannel, String code, Object... args) {
        if (sourceChannel == null) {
            return;
        }
        EmbedBuilder builder = getQueueMessage();
        builder.setDescription(messageService.getMessage(code, args));
        messageService.sendMessageSilent(sourceChannel::sendMessage, builder.build());
    }

    public void onNoMatches(long channelId, String query) {
        TextChannel textChannel = discordService.getShardManager().getTextChannelById(channelId);
        if (textChannel != null) {
            onNoMatches(textChannel, query);
        }
    }

    public void onNoMatches(MessageChannel sourceChannel, String query) {
        EmbedBuilder builder = getQueueMessage();
        builder.setDescription(messageService.getMessage("discord.command.audio.search.noMatches", query));
        messageService.sendMessageSilent(sourceChannel::sendMessage, builder.build());
    }

    public void onQueueError(long channelId, String code, Object... args) {
        TextChannel textChannel = discordService.getShardManager().getTextChannelById(channelId);
        if (textChannel != null) {
            onQueueError(textChannel, code, args);
        }
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

    public void onQueue(PlaybackInstance instance, MessageChannel sourceChannel, BotContext context, int pageNum) {
        List<TrackRequest> requests = instance.getQueue();
        if (requests.isEmpty()) {
            onEmptyQueue(sourceChannel);
            return;
        }

        final int pageSize = 25;
        List<List<TrackRequest>> parts = Lists.partition(requests, pageSize);
        final int totalPages = parts.size();
        final int offset = (pageNum - 1) * pageSize + 1 + instance.getCursor();

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
        if (instance.getCursor() > 0) {
            builder.setDescription(messageService.getMessage("discord.command.audio.queue.list.playlist.played",
                    instance.getCursor(), brandingService.getWebHost(), instance.getPlaylistUuid()));
        } else {
            builder.setDescription(messageService.getMessage("discord.command.audio.queue.list.playlist",
                    brandingService.getWebHost(), instance.getPlaylistUuid()));
        }
        for (int i = 0; i < pageRequests.size(); i++) {
            TrackRequest request = pageRequests.get(i);
            AudioTrack track = request.getTrack();
            AudioTrackInfo info = track.getInfo();

            int rowNum = i + offset;
            String title = messageService.getMessage("discord.command.audio.queue.list.entry", rowNum,
                    CommonUtils.formatDuration(track.getDuration()), rowNum == 1 ? ":musical_note: " : "",
                    getTitle(info), getUrl(info), getMemberName(request, false));
            builder.addField(EmbedBuilder.ZERO_WIDTH_SPACE, title, false);
        }

        String queueCommand = messageService.getMessageByLocale("discord.command.queue.key", context.getCommandLocale());

        builder.setFooter(totalPages > 1
                ? messageService.getMessage("discord.command.audio.queue.list.pageFooter",
                pageNum, totalPages, requests.size(), CommonUtils.formatDuration(totalDuration),
                context.getConfig().getPrefix(), queueCommand)
                : messageService.getMessage("discord.command.audio.queue.list.footer",
                requests.size(), CommonUtils.formatDuration(totalDuration)), null);
        messageService.sendMessageSilent(sourceChannel::sendMessage, builder.build());
    }

    private void runUpdater(TrackRequest request) {
        if (playRefreshInterval == null) {
            return;
        }
        syncByGuild(request, () -> {
            ScheduledFuture<?> task = scheduler.scheduleWithFixedDelay(() ->
                            contextService.withContext(request.getGuildId(), () -> updateMessage(request)),
                    playRefreshInterval);
            ScheduledFuture<?> oldTask = updaterTasks.put(request.getGuildId(), task);
            if (oldTask != null) {
                oldTask.cancel(true);
            }
        });
    }

    public void updateMessage(TrackRequest request) {
        TextChannel channel = request.getChannel();
        if (channel == null) {
            cancelUpdate(request);
            return;
        }
        try {
            if (request.isResetMessage()) {
                channel
                        .sendMessage(getPlayMessage(request).build())
                        .queue(m -> {
                            MessageController oldController = controllers.put(request.getGuildId(),
                                    new MessageController(context, m));
                            if (oldController != null) {
                                oldController.remove(false);
                            }
                            request.setResetMessage(false);
                        });
                return;
            }
            MessageController controller = controllers.get(request.getGuildId());
            if (controller != null) {
                controller.doForMessage(message -> {
                    if (message != null) {
                        message.editMessage(getPlayMessage(request).build()).queue(m -> {}, t -> {
                            if (t instanceof ErrorResponseException) {
                                handleUpdateError(request, (ErrorResponseException) t);
                            }
                        });
                    }
                });
            }
        } catch (PermissionException e) {
            LOGGER.warn("No permission to update", e);
            cancelUpdate(request);
        } catch (ErrorResponseException e) {
            handleUpdateError(request, e);
        }
    }

    private void handleUpdateError(TrackRequest request, ErrorResponseException e) {
        switch (e.getErrorResponse()) {
            case UNKNOWN_MESSAGE:
            case MISSING_ACCESS:
                cancelUpdate(request);
                break;
            default:
                LOGGER.error("Update message error", e);
                break;

        }
    }

    private EmbedBuilder getQueueMessage() {
        return messageService.getBaseEmbed().setTitle(messageService.getMessage("discord.command.audio.queue.title"), null);
    }

    private EmbedBuilder getPlayMessage(TrackRequest request) {
        EmbedBuilder builder = getBasicMessage(request);
        builder.setDescription(null);

        AudioTrackInfo info = request.getTrack().getInfo();

        PlaybackInstance instance = request.getTrack().getUserData(PlaybackInstance.class);

        String durationText;
        if (request.getEndReason() != null) {
            StringBuilder reasonBuilder = new StringBuilder();

            boolean hasDuration = !info.isStream && info.length > 0;
            if (hasDuration) {
                reasonBuilder.append(CommonUtils.formatDuration(request.getTrack().getDuration())).append(" (");
            }
            reasonBuilder.append(messageService.getEnumTitle(request.getEndReason()));

            String endMember = getMemberName(request, true);
            if (StringUtils.isNotBlank(endMember)) {
                reasonBuilder
                        .append(" - **")
                        .append(endMember)
                        .append("**");
            }
            if (hasDuration) {
                reasonBuilder.append(")");
            }
            reasonBuilder.append(CommonUtils.EMPTY_SYMBOL);
            durationText = reasonBuilder.toString();
        } else {
            durationText = getTextProgress(instance, request.getTrack());
        }

        if (instance.getPlaylistUuid() != null) {
            builder.setDescription(messageService.getMessage("discord.command.audio.panel.playlist",
                    brandingService.getWebHost(), instance.getPlaylistUuid()));
        }

        builder.addField(messageService.getMessage("discord.command.audio.panel.duration"),
                durationText, true);
        builder.addField(messageService.getMessage("discord.command.audio.panel.requestedBy"),
                getMemberName(request, false), true);

        if (request.getEndReason() == null) {
            if (instance.getPlayer().getVolume() != 100) {
                int volume = instance.getPlayer().getVolume();
                builder.addField(messageService.getMessage("discord.command.audio.panel.volume"),
                        String.format("%d%% %s", volume, CommonUtils.getVolumeIcon(volume)), true);
            }
            if (!RepeatMode.NONE.equals(instance.getMode())) {
                builder.addField(messageService.getMessage("discord.command.audio.panel.repeatMode"),
                        instance.getMode().getEmoji(), true);
            }
            if (instance.getPlayer().isPaused()) {
                builder.addField(messageService.getMessage("discord.command.audio.panel.paused"),
                        "\u23F8", true);
            }
        }
        return builder;
    }

    private EmbedBuilder getBasicMessage(TrackRequest request) {
        AudioTrackInfo info = request.getTrack().getInfo();
        EmbedBuilder builder = messageService.getBaseEmbed();
        builder.setTitle(getTitle(info), getUrl(info));
        String artworkUri = CommonUtils.getUrl(info.artworkUri);
        builder.setAuthor(getArtist(info), getUrl(info), artworkUri);
        builder.setThumbnail(artworkUri);
        builder.setDescription(messageService.getMessage("discord.command.audio.queue.add"));
        return builder;
    }

    private String getTextProgress(PlaybackInstance instance, AudioTrack track) {
        StringBuilder builder = new StringBuilder();
        boolean showPosition = featureSetService.isAvailable(instance.getGuildId());
        if (showPosition && instance.getPlayer().getPlayingTrack() != null) {
            builder.append(CommonUtils.formatDuration(instance.getPosition()));
        }
        if (!track.getInfo().isStream) {
            if (track.getDuration() >= 0) {
                if (showPosition && builder.length() > 0) {
                    builder.append("/");
                }
                builder.append(CommonUtils.formatDuration(track.getDuration()));
            }
        } else {
            builder.append(String.format(showPosition ? " (%s)" : "%s",
                    messageService.getMessage("discord.command.audio.panel.stream")));
        }
        return builder.toString();
    }

    public String getTitle(AudioTrackInfo info) {
        return MediaContainerDetection.UNKNOWN_TITLE.equals(info.title)
                ? messageService.getMessage("discord.command.audio.panel.unknownTitle") : info.title;
    }

    public String getArtist(AudioTrackInfo info) {
        return MediaContainerDetection.UNKNOWN_ARTIST.equals(info.author)
                ? messageService.getMessage("discord.command.audio.panel.unknownArtist") : info.author;
    }

    public String getUrl(AudioTrackInfo info) {
        return CommonUtils.getUrl(info.uri);
    }

    public void monitor(Set<Long> alive) {
        Set<Long> dead = new HashSet<>(updaterTasks.keySet());
        dead.removeAll(alive);
        if (CollectionUtils.isNotEmpty(dead)) {
            for (Long deadUpdater : dead) {
                try {
                    clear(deadUpdater);
                } catch (Exception e) {
                    LOGGER.warn("Could not clear dead updater");
                }
            }
        }
    }

    private void syncByGuild(TrackRequest request, Runnable action) {
        ReentrantLock lock = guildLocks.computeIfAbsent(request.getGuildId(), e -> new ReentrantLock());
        lock.lock();
        try {
            contextService.withContext(request.getGuildId(), action);
        } finally {
            lock.unlock();
            if (!lock.hasQueuedThreads()) {
                guildLocks.remove(request.getGuildId());
                if (lock.hasQueuedThreads()) {
                    guildLocks.put(request.getGuildId(), lock);
                }
            }
        }
    }

    private String getMemberName(TrackRequest request, boolean endMember) {
        if (endMember && request.getEndMemberId() == null) {
            return null;
        }
        long userId = endMember ? request.getEndMemberId() : request.getMemberId();
        ShardManager shardManager = discordService.getShardManager();
        Guild guild = shardManager.getGuildById(request.getGuildId());
        User user = shardManager.getUserById(userId);
        if (user != null && guild != null) {
            Member member = guild.getMember(user);
            if (member != null) {
                return member.getEffectiveName();
            }
        }
        return user != null ? user.getName() : String.valueOf(userId);
    }
}
