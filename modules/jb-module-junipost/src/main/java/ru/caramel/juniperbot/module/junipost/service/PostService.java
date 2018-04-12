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

import me.postaddict.instagram.scraper.model.Account;
import me.postaddict.instagram.scraper.model.Media;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.webhook.WebhookMessage;
import net.dv8tion.jda.webhook.WebhookMessageBuilder;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.caramel.juniperbot.core.persistence.repository.WebHookRepository;
import ru.caramel.juniperbot.core.service.DiscordService;
import ru.caramel.juniperbot.core.service.MessageService;
import ru.caramel.juniperbot.module.junipost.persistence.entity.JuniPost;
import ru.caramel.juniperbot.module.junipost.persistence.repository.JuniPostRepository;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PostService {

    public static final int MAX_DETAILED = 3;

    public static final String POST_URL = "https://www.instagram.com/p/";

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

    @Autowired
    private InstagramService instagramService;

    private long latestId;

    public void post(List<Media> medias, MessageChannel channel) {
        if (medias.size() > 0) {
            for (int i = 0; i < Math.min(MAX_DETAILED, medias.size()); i++) {
                EmbedBuilder builder = convertToEmbed(medias.get(i));
                messageService.sendMessageSilent(channel::sendMessage, builder.build());
            }
        }
    }

    public void onInstagramUpdated(List<Media> medias) {
        if (latestId != 0) {
            List<Media> newMedias = new ArrayList<>();
            for (Media media : medias) {
                if (media.getId() == latestId) {
                    break;
                }
                newMedias.add(media);
            }

            int size = Math.min(MAX_DETAILED, newMedias.size());
            if (size > 0) {
                List<MessageEmbed> embeds = newMedias.stream()
                        .map(e -> convertToEmbed(e).build())
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
        latestId = medias.get(0).getId();
    }

    public EmbedBuilder convertToEmbed(Media media) {
        EmbedBuilder builder = new EmbedBuilder()
                .setImage(media.getDisplayUrl())
                .setTimestamp(new Date(media.getTakenAtTimestamp()).toInstant())
                .setColor(messageService.getAccentColor());

        Account account = instagramService.getAccount();
        if (account != null) {
            builder.setAuthor(account.getFullName(), null, account.getProfilePicUrl());
        }

        if (media.getCaption() != null) {
            String text = media.getCaption();
            if (StringUtils.isNotEmpty(text)) {
                if (text.length() > MessageEmbed.EMBED_MAX_LENGTH_CLIENT) {
                    text = text.substring(0, MessageEmbed.EMBED_MAX_LENGTH_CLIENT - 1);
                }
                String link = POST_URL + media.getShortcode();
                if (media.getCaption().length() > 200) {
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
