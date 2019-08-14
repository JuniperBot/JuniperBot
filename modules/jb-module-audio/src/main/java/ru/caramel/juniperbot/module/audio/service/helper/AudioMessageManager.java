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
import lavalink.client.io.LavalinkSocket;
import lavalink.client.io.Link;
import lavalink.client.player.IPlayer;
import lavalink.client.player.LavalinkPlayer;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import ru.caramel.juniperbot.core.command.model.BotContext;
import ru.caramel.juniperbot.core.common.service.BrandingService;
import ru.caramel.juniperbot.core.common.service.DiscordService;
import ru.caramel.juniperbot.core.configuration.SchedulerConfiguration;
import ru.caramel.juniperbot.core.event.service.ContextService;
import ru.caramel.juniperbot.core.feature.service.FeatureSetService;
import ru.caramel.juniperbot.core.message.service.MessageService;
import ru.caramel.juniperbot.core.utils.CommonUtils;
import ru.caramel.juniperbot.core.utils.DiscordUtils;
import ru.caramel.juniperbot.module.audio.model.*;
import ru.caramel.juniperbot.module.audio.persistence.entity.MusicConfig;
import ru.caramel.juniperbot.module.audio.service.MusicConfigService;
import ru.caramel.juniperbot.module.audio.utils.MessageController;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
public class AudioMessageManager {

    private static final int MAX_SHORT_QUEUE = 3;

    @Value("${discord.audio.ui.refreshInterval:5000}")
    private Long playRefreshInterval;

    @Autowired
    @Qualifier(SchedulerConfiguration.COMMON_SCHEDULER_NAME)
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

    @Autowired
    private MusicConfigService musicConfigService;

    private Map<Long, ScheduledFuture<?>> updaterTasks = new ConcurrentHashMap<>();

    private Map<Long, MessageController> controllers = new ConcurrentHashMap<>();

    private Map<Long, ReentrantLock> guildLocks = new ConcurrentHashMap<>();

    public void onTrackAdd(TrackRequest request, PlaybackInstance instance) {
        if (instance.getCursor() >= 0) {
            TextChannel channel = request.getChannel();
            if (channel != null) {
                MessageEmbed addedMessage = getBasicMessage(request).build();
                if (isKeepMessage(request)) {
                    messageService.sendMessageSilent(channel::sendMessage, addedMessage);
                } else {
                    messageService.sendTempMessageSilent(channel::sendMessage, addedMessage, 10);
                }
                if (instance.getQueue().size() <= MAX_SHORT_QUEUE) {
                    MusicConfig config = musicConfigService.getByGuildId(request.getGuildId());
                    if (config != null && config.isShowQueue()) {
                        syncByGuild(request, () -> updateMessage(instance.getCurrent()));
                    }
                }
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
                                        () -> markAsPassed(request, oldController, isKeepMessage(request)));
                            }
                            if (isRefreshable(request.getGuildId())) {
                                runUpdater(request);
                            }
                        });
            } catch (PermissionException e) {
                log.warn("No permission to message", e);
            }
        });
    }

    public void onResetMessage(TrackRequest request) {
        request.setResetMessage(true);
        updateMessage(request);
    }

    public void onTrackEnd(TrackRequest request) {
        syncByGuild(request, () -> {
            cancelUpdate(request);
            controllers.computeIfPresent(request.getGuildId(), (g, c) -> {
                markAsPassed(request, c, isKeepMessage(request));
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
            log.warn("No permission to delete", e);
        } catch (ErrorResponseException e) {
            if (e.getErrorCode() != 10008 /* Unknown message */) {
                throw e;
            }
        }
    }

    private boolean isKeepMessage(TrackRequest request) {
        if (request.getEndReason() == EndReason.SHUTDOWN || request.getEndReason() == EndReason.ERROR) {
            return true;
        }
        MusicConfig musicConfig = musicConfigService.getByGuildId(request.getGuildId());
        return musicConfig == null || !musicConfig.isRemoveMessages();
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
            } else if (isRefreshable(request.getGuildId())) {
                runUpdater(request);
            } else {
                updateMessage(request);
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

        addQueue(builder, instance, pageRequests, offset, false);

        String queueCommand = messageService.getMessageByLocale("discord.command.queue.key", context.getCommandLocale());

        builder.setFooter(totalPages > 1
                ? messageService.getMessage("discord.command.audio.queue.list.pageFooter",
                pageNum, totalPages, requests.size(), CommonUtils.formatDuration(totalDuration),
                context.getConfig().getPrefix(), queueCommand)
                : messageService.getMessage("discord.command.audio.queue.list.footer",
                requests.size(), CommonUtils.formatDuration(totalDuration)), null);
        messageService.sendMessageSilent(sourceChannel::sendMessage, builder.build());
    }

    private void addQueue(EmbedBuilder builder, PlaybackInstance instance, List<TrackRequest> requests, int offset, boolean nextHint) {
        if (requests.isEmpty()) {
            return;
        }

        for (int i = 0; i < requests.size(); i++) {
            TrackRequest request = requests.get(i);
            AudioTrack track = request.getTrack();
            AudioTrackInfo info = track.getInfo();

            int rowNum = i + offset;
            String name = EmbedBuilder.ZERO_WIDTH_SPACE;
            if (nextHint && i == 0) {
                name = messageService.getMessage("discord.command.audio.queue.next");
            }

            String duration = info.isStream ? "" : String.format("`[%s]`", CommonUtils.formatDuration(track.getDuration()));
            String icon = info.isStream ? ":red_circle: " : ":musical_note: ";
            String title = messageService.getMessage("discord.command.audio.queue.list.entry", rowNum,
                    duration, !nextHint && rowNum - instance.getCursor() == 1 ? icon : "",
                    getTitle(info), getUrl(info), getMemberName(request, false));
            builder.addField(name, title, false);
        }
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
            log.warn("No permission to update", e);
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
                log.error("Update message error", e);
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
        PlaybackInstance instance = TrackData.get(request.getTrack()).getInstance();
        boolean refreshable = isRefreshable(instance.getGuildId());

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
            durationText = getTextProgress(instance, request.getTrack(), refreshable);
        }

        if (instance.getPlaylistUuid() != null) {
            builder.setDescription(messageService.getMessage("discord.command.audio.panel.playlist",
                    brandingService.getWebHost(), instance.getPlaylistUuid()));
        }

        int size = instance.getQueue().size();
        if (request.getEndReason() == null && size > 1) {
            MusicConfig config = musicConfigService.getByGuildId(instance.getGuildId());
            if (config != null && config.isShowQueue()) {
                List<TrackRequest> next = instance.getQueue().subList(1, Math.min(size, MAX_SHORT_QUEUE + 1));
                addQueue(builder, instance, next, 2 + instance.getCursor(), true);
            }
        }

        builder.addField(messageService.getMessage("discord.command.audio.panel.duration"),
                durationText, true);
        builder.addField(messageService.getMessage("discord.command.audio.panel.requestedBy"),
                getMemberName(request, false), true);

        if (request.getEndReason() == null) {
            IPlayer player = instance.getPlayer();
            if (player.getVolume() != 100) {
                int volume = player.getVolume();
                builder.addField(messageService.getMessage("discord.command.audio.panel.volume"),
                        String.format("%d%% %s", volume, CommonUtils.getVolumeIcon(volume)), true);
            }
            if (!RepeatMode.NONE.equals(instance.getMode())) {
                builder.addField(messageService.getMessage("discord.command.audio.panel.repeatMode"),
                        instance.getMode().getEmoji(), true);
            }
            if (player.isPaused()) {
                builder.addField(messageService.getMessage("discord.command.audio.panel.paused"),
                        "\u23F8", true);
            }

            if (player instanceof LavalinkPlayer) {
                LavalinkPlayer lavalinkPlayer = (LavalinkPlayer) player;
                Link link = lavalinkPlayer.getLink();
                if (link != null) {
                    LavalinkSocket socket = link.getNode(false);
                    if (socket != null) {
                        StringBuilder statsBuilder = new StringBuilder(messageService
                                .getMessage("discord.command.audio.panel.poweredBy", socket.getName()));
                        if (refreshable && socket.getStats() != null) {
                            long load = Math.round(socket.getStats().getSystemLoad() * 100);
                            if (load < 0) load = 0;
                            if (load > 100) load = 100;
                            statsBuilder
                                    .append(" ")
                                    .append(messageService.getMessage("discord.command.audio.panel.load", load));
                        }
                        builder.setFooter(statsBuilder.toString(), null);
                    }
                }
            }
        }
        return builder;
    }

    private boolean isRefreshable(long guildId) {
        MusicConfig config = musicConfigService.getByGuildId(guildId);
        return featureSetService.isAvailable(guildId) && (config != null && config.isAutoRefresh());
    }

    private EmbedBuilder getBasicMessage(TrackRequest request) {
        AudioTrackInfo info = request.getTrack().getInfo();
        EmbedBuilder builder = messageService.getBaseEmbed();
        builder.setTitle(getTitle(info), getUrl(info));
        String artworkUri = DiscordUtils.getUrl(TrackData.getArtwork(request.getTrack()));
        builder.setAuthor(getArtist(info), getUrl(info), artworkUri);
        builder.setThumbnail(artworkUri);
        builder.setDescription(messageService.getMessage("discord.command.audio.queue.add"));
        return builder;
    }

    private String getTextProgress(PlaybackInstance instance, AudioTrack track, boolean bonusActive) {
        StringBuilder builder = new StringBuilder();
        if (bonusActive && instance.getPlayer().getPlayingTrack() != null) {
            builder.append(CommonUtils.formatDuration(instance.getPosition()));
        }
        if (!track.getInfo().isStream) {
            if (track.getDuration() >= 0) {
                if (bonusActive && builder.length() > 0) {
                    builder.append("/");
                }
                builder.append(CommonUtils.formatDuration(track.getDuration()));
            }
        } else {
            builder.append(String.format(bonusActive ? " (%s)" : "%s",
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
        return DiscordUtils.getUrl(info.uri);
    }

    public void monitor(Set<Long> alive) {
        Set<Long> dead = new HashSet<>(updaterTasks.keySet());
        dead.removeAll(alive);
        if (CollectionUtils.isNotEmpty(dead)) {
            for (Long deadUpdater : dead) {
                try {
                    clear(deadUpdater);
                } catch (Exception e) {
                    log.warn("Could not clear dead updater");
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
