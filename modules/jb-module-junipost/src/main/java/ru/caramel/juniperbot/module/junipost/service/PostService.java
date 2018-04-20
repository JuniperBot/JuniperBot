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
package ru.caramel.juniperbot.module.junipost.service;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.webhook.WebhookMessage;
import net.dv8tion.jda.webhook.WebhookMessageBuilder;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.caramel.juniperbot.core.persistence.repository.WebHookRepository;
import ru.caramel.juniperbot.core.service.DiscordService;
import ru.caramel.juniperbot.core.service.MessageService;
import ru.caramel.juniperbot.module.junipost.model.InstagramMedia;
import ru.caramel.juniperbot.module.junipost.model.InstagramProfile;
import ru.caramel.juniperbot.module.junipost.persistence.entity.JuniPost;
import ru.caramel.juniperbot.module.junipost.persistence.repository.JuniPostRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PostService {

    public static final int MAX_DETAILED = 3;

    @Value("${instagram.post.userName:JuniperBot}")
    private String userName;

    @Autowired
    private JuniPostRepository juniPostRepository;

    @Autowired
    private WebHookRepository webHookRepository;

    @Autowired
    private DiscordService discordService;

    @Autowired
    private MessageService messageService;

    private long latestId;

    public void post(InstagramProfile profile, List<InstagramMedia> medias, MessageChannel channel) {
        if (medias.size() > 0) {
            for (int i = 0; i < Math.min(MAX_DETAILED, medias.size()); i++) {
                EmbedBuilder builder = convertToEmbed(profile, medias.get(i));
                messageService.sendMessageSilent(channel::sendMessage, builder.build());
            }
        }
    }

    public void onInstagramUpdated(InstagramProfile profile) {
        if (latestId != 0) {
            List<InstagramMedia> newMedias = new ArrayList<>();
            for (InstagramMedia media : profile.getFeed()) {
                if (media.getId() == latestId) {
                    break;
                }
                newMedias.add(media);
            }

            int size = Math.min(MAX_DETAILED, newMedias.size());
            if (size > 0) {
                List<MessageEmbed> embeds = newMedias.stream()
                        .map(e -> convertToEmbed(profile, e).build())
                        .collect(Collectors.toList());

                WebhookMessage message = new WebhookMessageBuilder()
                        .setAvatarUrl(discordService.getJda().getSelfUser().getAvatarUrl())
                        .setUsername(userName)
                        .addEmbeds(embeds)
                        .build();

                List<JuniPost> juniPosts = juniPostRepository.findActive();
                juniPosts.forEach(e -> discordService.executeWebHook(e.getWebHook(), message, e2 -> {
                    e2.setEnabled(false);
                    webHookRepository.save(e2);
                }));
            }
        }
        if (CollectionUtils.isNotEmpty(profile.getFeed())) {
            latestId = profile.getFeed().get(0).getId();
        }
    }

    public EmbedBuilder convertToEmbed(InstagramProfile profile, InstagramMedia media) {
        EmbedBuilder builder = new EmbedBuilder()
                .setImage(media.getImageUrl())
                .setTimestamp(media.getDate().toInstant())
                .setColor(messageService.getAccentColor())
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
}
