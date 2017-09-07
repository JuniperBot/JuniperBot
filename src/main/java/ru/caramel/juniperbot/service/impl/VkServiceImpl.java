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
package ru.caramel.juniperbot.service.impl;

import com.vk.api.sdk.callback.objects.messages.CallbackMessage;
import com.vk.api.sdk.callback.objects.wall.CallbackWallPost;
import com.vk.api.sdk.objects.audio.AudioFull;
import com.vk.api.sdk.objects.base.Link;
import com.vk.api.sdk.objects.docs.Doc;
import com.vk.api.sdk.objects.docs.DocPreviewPhoto;
import com.vk.api.sdk.objects.pages.WikipageFull;
import com.vk.api.sdk.objects.photos.Photo;
import com.vk.api.sdk.objects.photos.PhotoAlbum;
import com.vk.api.sdk.objects.photos.PhotoSizes;
import com.vk.api.sdk.objects.polls.Poll;
import com.vk.api.sdk.objects.video.Video;
import com.vk.api.sdk.objects.wall.Graffiti;
import com.vk.api.sdk.objects.wall.PostType;
import com.vk.api.sdk.objects.wall.PostedPhoto;
import com.vk.api.sdk.objects.wall.WallpostAttachment;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.caramel.juniperbot.integration.discord.DiscordClient;
import ru.caramel.juniperbot.integration.discord.model.WebHookMessage;
import ru.caramel.juniperbot.model.enums.VkConnectionStatus;
import ru.caramel.juniperbot.model.enums.WebHookType;
import ru.caramel.juniperbot.persistence.entity.GuildConfig;
import ru.caramel.juniperbot.persistence.entity.VkConnection;
import ru.caramel.juniperbot.persistence.entity.WebHook;
import ru.caramel.juniperbot.persistence.repository.VkConnectionRepository;
import ru.caramel.juniperbot.persistence.repository.WebHookRepository;
import ru.caramel.juniperbot.service.MessageService;
import ru.caramel.juniperbot.service.VkService;
import ru.caramel.juniperbot.service.WebHookService;

import java.util.*;
import java.util.stream.Collectors;

import static ru.caramel.juniperbot.utils.CommonUtils.coalesce;
import static ru.caramel.juniperbot.utils.CommonUtils.trimTo;
import static ru.caramel.juniperbot.utils.CommonUtils.mdLink;

@Service
public class VkServiceImpl implements VkService {

    private final static String CLUB_URL = "https://vk.com/club%s";

    private final static String WALL_URL = "https://vk.com/wall-%s_%s";

    private final static String PHOTO_URL = WALL_URL + "?z=photo-%s_%s";

    private final static String VIDEO_URL = WALL_URL + "?z=video-%s_%s";

    private final static String ALBUM_URL = "https://vk.com/album-%s_%s";

    private final static Map<Integer, String> DOC_TYPE_NAMES;

    static {
        Map<Integer, String> types = new HashMap<>();
        types.put(1, "vk.message.documentType.text");
        types.put(2, "vk.message.documentType.archive");
        types.put(3, "vk.message.documentType.gif");
        types.put(4, "vk.message.documentType.picture");
        types.put(5, "vk.message.documentType.audio");
        types.put(6, "vk.message.documentType.video");
        types.put(7, "vk.message.documentType.money");
        types.put(8, "vk.message.documentType.unknown");
        DOC_TYPE_NAMES = Collections.unmodifiableMap(types);
    }

    @Autowired
    private DiscordClient discordClient;

    @Autowired
    private VkConnectionRepository repository;

    @Autowired
    private WebHookRepository hookRepository;

    @Autowired
    private MessageService messageService;

    @Autowired
    private WebHookService webHookService;

    @Override
    @Transactional
    public VkConnection create(GuildConfig config, String name, String code) {
        VkConnection connection = new VkConnection();
        connection.setConfig(config);
        connection.setStatus(VkConnectionStatus.CONFIRMATION);
        connection.setToken(UUID.randomUUID().toString());
        connection.setName(name);
        connection.setConfirmCode(code);

        WebHook hook = new WebHook();
        hook.setType(WebHookType.VK);
        hook.setEnabled(true);
        connection.setWebHook(hook);
        return repository.save(connection);
    }

    @Override
    @Transactional
    public void delete(GuildConfig config, long id) {
        VkConnection connection = repository.getOne(id);
        if (!connection.getConfig().equals(config)) {
            throw new IllegalStateException("Trying to delete not own connection!");
        }
        repository.delete(connection);
        webHookService.delete(config.getGuildId(), connection.getWebHook());
    }

    @Override
    public VkConnection getForToken(String token) {
        return repository.findByToken(token);
    }

    @Override
    public String confirm(VkConnection connection, CallbackMessage message) {
        connection.setGroupId(message.getGroupId());
        connection.setStatus(VkConnectionStatus.CONNECTED);
        return repository.save(connection).getConfirmCode();
    }

    @Override
    public void post(VkConnection connection, CallbackMessage<CallbackWallPost> message) {
        if (!connection.getWebHook().isValid()) {
            return;
        }
        discordClient.executeWebHook(connection.getWebHook(), createMessage(message), e -> {
            e.setEnabled(false);
            hookRepository.save(e);
        });
    }

    private WebHookMessage createMessage(CallbackMessage<CallbackWallPost> message) {
        CallbackWallPost post = message.getObject();
        if (PostType.SUGGEST.equals(post.getPostType())) {
            return null; // do not post suggestions
        }

        List<EmbedBuilder> embeds = CollectionUtils.isNotEmpty(post.getAttachments())
                ? post.getAttachments().stream()
                .map(e -> getAttachmentEmbed(message, e))
                .filter(Objects::nonNull)
                .collect(Collectors.toList()) : new ArrayList<>();
        if (embeds.size() > 10) {
            embeds = embeds.subList(0, 10); // max 10 embeds in message
        }

        String content = trimTo(post.getText(), 2000);
        EmbedBuilder contentEmbed = null;
        if (embeds.size() == 1) {
            contentEmbed = embeds.get(0);
        } else if (embeds.isEmpty() && StringUtils.isNotEmpty(content)) {
            contentEmbed = messageService.getBaseEmbed();
            embeds.add(contentEmbed);
        }
        if (contentEmbed != null) {
            if (post.getDate() != null) {
                contentEmbed.setTimestamp(new Date(((long) post.getDate()) * 1000).toInstant());
            }
            String url = String.format(WALL_URL, message.getGroupId(), post.getId());
            setText(contentEmbed, post.getText(), url);
            content = null; // show it on embed instead
        }

        return WebHookMessage.builder()
                .content(content)
                .embeds(embeds.isEmpty() ? null : embeds.stream().map(EmbedBuilder::build).collect(Collectors.toList()))
                .build();
    }

    private EmbedBuilder getAttachmentEmbed(CallbackMessage<CallbackWallPost> message, WallpostAttachment attachment) {
        EmbedBuilder builder = messageService.getBaseEmbed()
                .setFooter(String.format(CLUB_URL, message.getGroupId()), null);
        switch (attachment.getType()) {
            case PHOTO:
                Photo photo = attachment.getPhoto();
                if (photo == null) {
                    return null;
                }
                setPhoto(builder, message, photo, true);
                return builder;
            case POSTED_PHOTO:
                PostedPhoto postedPhoto = attachment.getPostedPhoto();
                if (postedPhoto == null) {
                    return null;
                }
                builder.setImage(coalesce(postedPhoto.getPhoto604(),
                        postedPhoto.getPhoto130()));
                return builder;
            case VIDEO:
                Video video = attachment.getVideo();
                if (video == null) {
                    return null;
                }
                String url = String.format(VIDEO_URL,
                        Math.abs(message.getGroupId()),
                        message.getObject().getId(),
                        Math.abs(video.getOwnerId()),
                        video.getId());

                setText(builder, video.getTitle(), url);
                builder.setImage(coalesce(video.getPhoto800(), video.getPhoto320(), video.getPhoto130()));
                if (video.getDate() != null) {
                    builder.setTimestamp(new Date(((long) video.getDate()) * 1000).toInstant());
                }
                return builder;
            case AUDIO:
                AudioFull audio = attachment.getAudio();
                if (audio == null) {
                    return null;
                }
                if (StringUtils.isNotEmpty(audio.getArtist())) {
                    builder.addField(messageService.getMessage("vk.message.audio.artist"), audio.getArtist(), true);
                }
                if (StringUtils.isNotEmpty(audio.getTitle())) {
                    builder.addField(messageService.getMessage("vk.message.audio.title"), audio.getTitle(), true);
                }
                if (builder.getFields().isEmpty()) {
                    return null;
                }
                if (audio.getDate() != null) {
                    builder.setTimestamp(new Date(((long) audio.getDate()) * 1000).toInstant());
                }
                return builder;

            case DOC:
                Doc doc = attachment.getDoc();
                if (doc == null) {
                    return null;
                }
                String type = DOC_TYPE_NAMES.get(doc.getType());
                if (type == null) {
                    return null;
                }
                String name = mdLink(doc.getTitle(), doc.getUrl());
                builder.addField(messageService.getMessage("vk.message.documentType"),
                        messageService.getMessage(type), true);
                builder.addField(messageService.getMessage("vk.message.documentType.download"), name, true);
                if ((doc.getType() == 3 || doc.getType() == 4)
                        && doc.getPreview() != null && doc.getPreview().getPhoto() != null
                        && CollectionUtils.isNotEmpty(doc.getPreview().getPhoto().getSizes())) {
                    DocPreviewPhoto preview = doc.getPreview().getPhoto();
                    Integer size = 0;
                    for (PhotoSizes sizes : preview.getSizes()) {
                        Integer total = sizes.getWidth() * sizes.getHeight();
                        if (total > size && sizes.getSrc() != null) {
                            size = total;
                            builder.setImage(sizes.getSrc());
                        }
                    }
                }
                return builder;
            case GRAFFITI:
                Graffiti graffiti = attachment.getGraffiti();
                if (graffiti == null) {
                    return null;
                }
                builder.setImage(coalesce(graffiti.getPhoto586(),
                        graffiti.getPhoto200()));
                return builder;
            case LINK:
                Link link = attachment.getLink();
                if (link == null) {
                    return null;
                }
                builder.addField(messageService.getMessage("vk.message.link.title"),
                        trimTo(mdLink(link.getTitle(), link.getUrl()), MessageEmbed.TEXT_MAX_LENGTH), true);
                if (StringUtils.isNotEmpty(link.getCaption())) {
                    builder.addField(messageService.getMessage("vk.message.link.source"), link.getCaption(), true);
                }
                if (link.getPhoto() != null) {
                    setPhoto(builder, message, link.getPhoto(), false);
                }
                return builder;
            case POLL:
                Poll poll = attachment.getPoll();
                if (poll == null) {
                    return null;
                }

                StringBuilder answers = new StringBuilder();
                for (int i = 0; i < poll.getAnswers().size(); i++) {
                    answers.append(i + 1).append(". ").append(poll.getAnswers().get(0).getText()).append('\n');
                }
                builder.addField(messageService.getMessage("vk.message.poll"),
                        trimTo(poll.getQuestion(), MessageEmbed.TEXT_MAX_LENGTH), false);
                builder.addField(messageService.getMessage("vk.message.poll.answers"),
                        trimTo(answers.toString(), MessageEmbed.TEXT_MAX_LENGTH), false);
                return builder;
            case PAGE:
                WikipageFull page = attachment.getPage();
                if (page == null) {
                    return null;
                }
                builder.addField(messageService.getMessage("vk.message.page"),
                        trimTo(mdLink(page.getTitle(), page.getViewUrl()), MessageEmbed.TEXT_MAX_LENGTH), false);
                return builder;
            case ALBUM:
                PhotoAlbum album = attachment.getAlbum();
                if (album == null) {
                    return null;
                }
                url = String.format(ALBUM_URL, message.getGroupId(), album.getId());

                builder.addField(messageService.getMessage("vk.message.album"),
                        trimTo(mdLink(album.getTitle(), url), MessageEmbed.TEXT_MAX_LENGTH), true);
                builder.addField(messageService.getMessage("vk.message.album.photos"),
                        String.valueOf(album.getSize()), true);
                builder.setDescription(album.getDescription());
                if (album.getThumb() != null) {
                    setPhoto(builder, message, album.getThumb(), false);
                }

                return builder;
        }
        return null;
    }

    private void setPhoto(EmbedBuilder builder, CallbackMessage<CallbackWallPost> message, Photo photo, boolean showText) {
        String url = String.format(PHOTO_URL,
                Math.abs(message.getGroupId()),
                message.getObject().getId(),
                Math.abs(photo.getOwnerId()),
                photo.getId());

        if (showText) {
            setText(builder, photo.getText(), url);
        }
        builder.setImage(coalesce(photo.getPhoto2560(),
                photo.getPhoto1280(),
                photo.getPhoto807(),
                photo.getPhoto604(),
                photo.getPhoto130(),
                photo.getPhoto75()));
        if (photo.getDate() != null) {
            builder.setTimestamp(new Date(((long) photo.getDate()) * 1000).toInstant());
        }
    }

    private void setText(EmbedBuilder builder, String text, String url) {
        if (StringUtils.isNotEmpty(text)) {
            if (text.length() < 100) {
                builder.setTitle(trimTo(text, MessageEmbed.TITLE_MAX_LENGTH), url);
            } else {
                builder.setTitle(messageService.getMessage("vk.message.open"), url);
                builder.setDescription(trimTo(text, MessageEmbed.TEXT_MAX_LENGTH));
            }
        }
    }
}
