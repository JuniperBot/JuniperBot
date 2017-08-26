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
import ru.caramel.juniperbot.model.VkConnectionStatus;
import ru.caramel.juniperbot.model.WebHookType;
import ru.caramel.juniperbot.persistence.entity.GuildConfig;
import ru.caramel.juniperbot.persistence.entity.VkConnection;
import ru.caramel.juniperbot.persistence.entity.WebHook;
import ru.caramel.juniperbot.persistence.repository.VkConnectionRepository;
import ru.caramel.juniperbot.persistence.repository.WebHookRepository;
import ru.caramel.juniperbot.service.MessageService;
import ru.caramel.juniperbot.service.VkService;

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
        types.put(1, "Текстовый документ");
        types.put(2, "Архив");
        types.put(3, "GIF-анимация");
        types.put(4, "Изображение");
        types.put(5, "Аудио");
        types.put(6, "Видео");
        types.put(7, "Электронные деньги");
        types.put(8, "Неизвестный");
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
                    builder.addField("Исполнитель", audio.getArtist(), true);
                }
                if (StringUtils.isNotEmpty(audio.getTitle())) {
                    builder.addField("Название композиции", audio.getTitle(), true);
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
                builder.addField("Тип документа", type, true);
                builder.addField("Скачать документ", name, true);
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
                builder.addField("Ссылка", trimTo(mdLink(link.getTitle(), link.getUrl()), MessageEmbed.TEXT_MAX_LENGTH), true);
                if (StringUtils.isNotEmpty(link.getCaption())) {
                    builder.addField("Источник", link.getCaption(), true);
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
                builder.addField("Опрос", trimTo(poll.getQuestion(), MessageEmbed.TEXT_MAX_LENGTH), false);
                builder.addField("Варианты ответов", trimTo(answers.toString(), MessageEmbed.TEXT_MAX_LENGTH), false);
                return builder;
            case PAGE:
                WikipageFull page = attachment.getPage();
                if (page == null) {
                    return null;
                }
                builder.addField("Страница", trimTo(mdLink(page.getTitle(), page.getViewUrl()), MessageEmbed.TEXT_MAX_LENGTH), false);
                return builder;
            case ALBUM:
                PhotoAlbum album = attachment.getAlbum();
                if (album == null) {
                    return null;
                }
                url = String.format(ALBUM_URL, message.getGroupId(), album.getId());

                builder.addField("Альбом", trimTo(mdLink(album.getTitle(), url), MessageEmbed.TEXT_MAX_LENGTH), true);
                builder.addField("Фотографий", String.valueOf(album.getSize()), true);
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
                builder.setTitle("Открыть на стене", url);
                builder.setDescription(trimTo(text, MessageEmbed.TEXT_MAX_LENGTH));
            }
        }
    }
}
