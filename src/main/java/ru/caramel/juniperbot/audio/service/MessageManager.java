package ru.caramel.juniperbot.audio.service;

import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import ru.caramel.juniperbot.configuration.DiscordConfig;

import java.awt.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import static org.apache.commons.lang3.time.DurationFormatUtils.formatDuration;

@Service
public class MessageManager {

    private final Map<AudioTrack, TextChannel> sources = new HashMap<>();

    private final Map<AudioTrack, User> authors = new HashMap<>();

    private final Map<AudioTrack, Message> messages = new HashMap<>();

    private final Map<AudioTrack, ScheduledFuture<?>> updaters = new HashMap<>();

    private TextChannel lastChannel;

    @Autowired
    private DiscordConfig discordConfig;

    @Autowired
    private TaskScheduler scheduler;

    public void onTrackAdd(AudioTrack track, TextChannel channel, User requestedBy, boolean silent) {
        lastChannel = channel;
        sources.put(track, channel);
        authors.put(track, requestedBy);
        if (!silent) {
            channel.sendMessage(getBasicMessage(track).build()).queue();
        }
    }

    public void onTrackStart(AudioTrack track) {
        TextChannel channel = sources.get(track);
        if (channel != null) {
            channel.sendMessage(getPlayMessage(track).build()).queue(e -> runUpdater(track, e));
        }
    }

    public void onTrackEnd(AudioTrack track) {
        sources.remove(track);
        authors.remove(track);
        ScheduledFuture<?> task = updaters.remove(track);
        if (task != null) {
            task.cancel(false);
        }
        Message message = messages.remove(track);
        if (message != null) {
            message.delete().complete();
        }
    }

    public void onQueueEnd() {
        if (lastChannel != null) {
            EmbedBuilder builder = getQueueMessage();
            builder.setDescription("Достигнут конец очереди воспроизведения :musical_note:");
            lastChannel.sendMessage(builder.build()).queue();
        }
    }

    public void onNoMatches(TextChannel sourceChannel, String query) {
        EmbedBuilder builder = getQueueMessage();
        builder.setDescription(String.format("По запросу **%s** ничего не найдено. :grey_question:", query));
        sourceChannel.sendMessage(builder.build()).queue();
    }

    public void onError(TextChannel sourceChannel, FriendlyException e) {
        EmbedBuilder builder = getQueueMessage();
        builder.setColor(Color.RED);
        builder.setDescription(String.format("Произошла ошибка :interrobang:: %s", e.getMessage()));
        sourceChannel.sendMessage(builder.build()).queue();
    }

    private void runUpdater(AudioTrack track, Message message) {
        messages.put(track, message);
        if (discordConfig.getPlayRefreshInterval() != null) {
            ScheduledFuture<?> task = scheduler.scheduleWithFixedDelay(() -> {
                Message msg = messages.get(track);
                if (msg != null) {
                    try {
                        msg.editMessage(getPlayMessage(track).build()).complete();
                    } catch (Exception e) {
                        // fall down and skip
                    }
                }
            }, discordConfig.getPlayRefreshInterval());
            updaters.put(track, task);
        }
    }

    private EmbedBuilder getQueueMessage() {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("Очередь воспроизведения", null);
        builder.setColor(discordConfig.getAccentColor());
        return builder;
    }

    private EmbedBuilder getPlayMessage(AudioTrack track) {
        EmbedBuilder builder = getBasicMessage(track);
        builder.setDescription(null);
        builder.addField("Длительность", getTextProgress(track), true);
        User user = authors.get(track);
        if (user != null) {
            builder.addField("Поставил", user.getName(), true);
        }
        return builder;
    }

    private EmbedBuilder getBasicMessage(AudioTrack track) {
        AudioTrackInfo info = track.getInfo();
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
}
