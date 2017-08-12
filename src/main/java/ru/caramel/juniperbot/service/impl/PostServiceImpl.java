package ru.caramel.juniperbot.service.impl;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.exceptions.PermissionException;
import org.apache.commons.lang3.StringUtils;
import org.jinstagram.entity.users.feed.MediaFeedData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.caramel.juniperbot.configuration.DiscordConfig;
import ru.caramel.juniperbot.integration.discord.DiscordClient;
import ru.caramel.juniperbot.integration.instagram.InstagramListener;
import ru.caramel.juniperbot.persistence.entity.AutoPost;
import ru.caramel.juniperbot.persistence.repository.AutoPostRepository;
import ru.caramel.juniperbot.service.PostService;

import java.util.*;

@Service
public class PostServiceImpl implements PostService, InstagramListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostServiceImpl.class);

    @Autowired
    private AutoPostRepository repository;

    @Autowired
    private DiscordConfig discordConfig;

    @Autowired
    private DiscordClient discordClient;

    @Override
    @Transactional
    public boolean subscribe(TextChannel channel) {
        String channelId = channel.getId();
        String guildId = channel.getGuild().getId();
        return !repository.exists(guildId, channelId) && repository.save(new AutoPost(guildId, channelId)).getId() != null;
    }

    @Override
    @Transactional
    public boolean unSubscribe(TextChannel channel) {
        return repository.deleteByGuildIdAndChannelId(channel.getGuild().getId(), channel.getId()) > 0;
    }

    @Override
    @Transactional
    public boolean switchSubscription(TextChannel channel) {
        boolean subscribed = subscribe(channel);
        if (!subscribed) {
            unSubscribe(channel);
        }
        return subscribed;
    }

    @Override
    public synchronized void onInstagramUpdated(List<MediaFeedData> medias) {
        Map<String, Guild> guildCache = new HashMap<>();
        Map<TextChannel, AutoPost> targetMap = new HashMap<>();
        List<AutoPost> toDelete = new ArrayList<>();

        JDA jda = discordClient.getJda();
        if (jda == null || !JDA.Status.CONNECTED.equals(jda.getStatus())) {
            return;
        }

        for (AutoPost post : repository.findAll()) {
            Guild guild = null;
            if (!guildCache.containsKey(post.getGuildId())) {
                guild = guildCache.computeIfAbsent(post.getGuildId(), jda::getGuildById);
            }

            if (guild == null) {
                toDelete.add(post);
                continue;
            }

            TextChannel channel = guild.getTextChannelById(post.getChannelId());
            if (channel == null) {
                toDelete.add(post);
                continue;
            }
            targetMap.put(channel, post);
        }

        targetMap.forEach((channel, post) -> {
            if (post.getLatestId() != null) {
                List<MediaFeedData> newMedias = new ArrayList<>();
                for (MediaFeedData media : medias) {
                    if (media.getId().equals(post.getLatestId())) {
                        break;
                    }
                    newMedias.add(media);
                }

                if (!newMedias.isEmpty()) {
                    try {
                        post(newMedias, channel);
                    } catch (PermissionException e) {
                        LOGGER.warn("No permissions to send {}", post, e);
                    }
                }
            }
            post.setLatestId(medias.get(0).getId());
        });
        repository.delete(toDelete);
        repository.save(targetMap.values());
    }

    @Override
    public void post(List<MediaFeedData> medias, MessageChannel channel) {
        if (medias.size() > 0) {
            for (int i = 0; i < Math.min(DiscordConfig.MAX_DETAILED, medias.size()); i++) {
                EmbedBuilder builder = convertToEmbed(medias.get(i));
                channel.sendMessage(builder.build()).queue();
            }
        }
    }

    @Override
    public EmbedBuilder convertToEmbed(MediaFeedData media) {
        EmbedBuilder builder = new EmbedBuilder()
                .setImage(media.getImages().getStandardResolution().getImageUrl())
                .setAuthor(media.getUser().getFullName(), null, media.getUser().getProfilePictureUrl())
                .setTimestamp(new Date(Long.parseLong(media.getCreatedTime()) * 1000).toInstant())
                .setColor(discordConfig.getAccentColor());

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
