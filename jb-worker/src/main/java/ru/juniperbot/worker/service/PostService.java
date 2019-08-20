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
package ru.juniperbot.worker.service;

import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import club.minnced.discord.webhook.send.WebhookMessage;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import lombok.Getter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.juniperbot.common.persistence.entity.JuniPost;
import ru.juniperbot.common.persistence.repository.JuniPostRepository;
import ru.juniperbot.common.persistence.repository.WebHookRepository;
import ru.juniperbot.common.utils.WebhookUtils;
import ru.juniperbot.common.worker.event.service.ContextService;
import ru.juniperbot.common.worker.message.service.MessageService;
import ru.juniperbot.common.worker.shared.model.InstagramMedia;
import ru.juniperbot.common.worker.shared.model.InstagramProfile;
import ru.juniperbot.common.worker.shared.service.DiscordService;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PostService {

    public static final int MAX_DETAILED = 3;

    @Value("${spring.application.name}")
    private String userName;

    @Autowired
    private JuniPostRepository juniPostRepository;

    @Autowired
    private WebHookRepository webHookRepository;

    @Autowired
    private DiscordService discordService;

    @Autowired
    private ContextService contextService;

    @Autowired
    private MessageService messageService;

    private long latestId;

    @Getter
    @Value("${instagram.pollUserName:juniperfoxx}")
    private String accountName;

    @Getter
    private String iconUrl;

    public void post(InstagramProfile profile, List<InstagramMedia> medias, MessageChannel channel) {
        if (medias.size() > 0) {
            for (int i = 0; i < Math.min(MAX_DETAILED, medias.size()); i++) {
                EmbedBuilder builder = convertToEmbed(profile, medias.get(i));
                messageService.sendMessageSilent(channel::sendMessage, builder.build());
            }
        }
    }

    public void onInstagramUpdated(InstagramProfile profile) {
        if (CollectionUtils.isEmpty(profile.getFeed())) {
            return;
        }
        accountName = profile.getFullName();
        iconUrl = profile.getImageUrl();
        if (profile.getFeed().stream().anyMatch(e -> e.getId() == latestId)) {
            List<InstagramMedia> newMedias = new ArrayList<>();
            for (InstagramMedia media : profile.getFeed()) {
                if (media.getId() == latestId) {
                    break;
                }
                newMedias.add(media);
            }

            int size = Math.min(MAX_DETAILED, newMedias.size());
            if (size > 0) {
                List<WebhookEmbed> embeds = newMedias.stream()
                        .map(e -> convertToWebhookEmbed(profile, e).build())
                        .collect(Collectors.toList());

                WebhookMessage message = new WebhookMessageBuilder()
                        .setAvatarUrl(discordService.getJda().getSelfUser().getAvatarUrl())
                        .setUsername(userName)
                        .addEmbeds(embeds)
                        .build();

                List<JuniPost> juniPosts = juniPostRepository.findActive();
                juniPosts.forEach(e -> WebhookUtils.sendWebhook(e.getWebHook(), message, e2 -> {
                    e2.setEnabled(false);
                    webHookRepository.save(e2);
                }));
            }
        }
        latestId = profile.getFeed().get(0).getId();
    }

    public EmbedBuilder convertToEmbed(InstagramProfile profile, InstagramMedia media) {
        EmbedBuilder builder = new EmbedBuilder()
                .setImage(media.getImageUrl())
                .setTimestamp(media.getDate().toInstant())
                .setColor(contextService.getColor())
                .setAuthor(profile.getFullName(), null, profile.getImageUrl());

        if (media.getText() != null) {
            String text = media.getText();
            if (StringUtils.isNotEmpty(text)) {
                if (text.length() > MessageEmbed.EMBED_MAX_LENGTH_CLIENT) {
                    text = text.substring(0, MessageEmbed.EMBED_MAX_LENGTH_CLIENT - 1);
                }
                String link = media.getLink();
                if (media.getText().length() > 200) {
                    builder.setTitle(link, link);
                    builder.setDescription(text);
                } else {
                    builder.setTitle(text, link);
                }
            }
        }
        return builder;
    }

    public WebhookEmbedBuilder convertToWebhookEmbed(InstagramProfile profile, InstagramMedia media) {
        WebhookEmbedBuilder builder = new WebhookEmbedBuilder()
                .setImageUrl(media.getImageUrl())
                .setTimestamp(media.getDate().toInstant())
                .setColor(contextService.getColor().getRGB())
                .setAuthor(new WebhookEmbed.EmbedAuthor(profile.getFullName(), profile.getImageUrl(), null));

        if (media.getText() != null) {
            String text = media.getText();
            if (StringUtils.isNotEmpty(text)) {
                if (text.length() > MessageEmbed.EMBED_MAX_LENGTH_CLIENT) {
                    text = text.substring(0, MessageEmbed.EMBED_MAX_LENGTH_CLIENT - 1);
                }
                String link = media.getLink();
                if (media.getText().length() > 200) {
                    builder.setTitle(new WebhookEmbed.EmbedTitle(link, link));
                    builder.setDescription(text);
                } else {
                    builder.setTitle(new WebhookEmbed.EmbedTitle(text, link));
                }
            }
        }
        return builder;
    }
}
