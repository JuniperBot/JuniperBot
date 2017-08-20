package ru.caramel.juniperbot.service.impl;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.MessageEmbed;
import org.apache.commons.lang3.StringUtils;
import org.jinstagram.entity.users.feed.MediaFeedData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.caramel.juniperbot.configuration.DiscordConfig;
import ru.caramel.juniperbot.integration.discord.DiscordClient;
import ru.caramel.juniperbot.integration.discord.model.WebHookMessage;
import ru.caramel.juniperbot.integration.instagram.InstagramListener;
import ru.caramel.juniperbot.persistence.entity.WebHook;
import ru.caramel.juniperbot.persistence.repository.WebHookRepository;
import ru.caramel.juniperbot.service.PostService;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PostServiceImpl implements PostService, InstagramListener {

    @Autowired
    private WebHookRepository webHookRepository;

    @Autowired
    private DiscordConfig config;

    @Autowired
    private DiscordClient client;

    private String latestId;


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
    public void onInstagramUpdated(List<MediaFeedData> medias) {
        if (latestId != null) {
            List<MediaFeedData> newMedias = new ArrayList<>();
            for (MediaFeedData media : medias) {
                if (media.getId().equals(latestId)) {
                    break;
                }
                newMedias.add(media);
            }

            int size = Math.min(DiscordConfig.MAX_DETAILED, newMedias.size());
            if (size > 0) {
                List<MessageEmbed> embeds = newMedias.stream()
                        .map(e -> convertToEmbed(e).build())
                        .collect(Collectors.toList());

                WebHookMessage message = WebHookMessage.builder()
                        .avatarUrl(config.getAvatarUrl())
                        .username(config.getUserName())
                        .embeds(embeds)
                        .build();

                List<WebHook> webHooks = webHookRepository.findActive();
                webHooks.forEach(e -> client.executeWebHook(e, message, e2 -> {
                    e2.setEnabled(false);
                    webHookRepository.save(e2);
                }));
            }
        }
        latestId = medias.get(0).getId();
    }

    @Override
    public EmbedBuilder convertToEmbed(MediaFeedData media) {
        EmbedBuilder builder = new EmbedBuilder()
                .setImage(media.getImages().getStandardResolution().getImageUrl())
                .setAuthor(media.getUser().getFullName(), null, media.getUser().getProfilePictureUrl())
                .setTimestamp(new Date(Long.parseLong(media.getCreatedTime()) * 1000).toInstant())
                .setColor(config.getAccentColor());

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
