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
package ru.juniperbot.api.subscriptions.integrations;

import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import club.minnced.discord.webhook.send.WebhookMessage;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import ru.juniperbot.common.configuration.CommonProperties;
import ru.juniperbot.common.model.InstagramMedia;
import ru.juniperbot.common.model.InstagramProfile;
import ru.juniperbot.common.persistence.entity.JuniPost;
import ru.juniperbot.common.persistence.repository.JuniPostRepository;
import ru.juniperbot.common.persistence.repository.WebHookRepository;
import ru.juniperbot.common.utils.CommonUtils;
import ru.juniperbot.common.utils.WebhookUtils;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class InstagramService {

    private final Object $recentLock = new Object[0];

    public static final String ROOT_URL = "https://www.instagram.com/";

    public static final String POST_URL = "https://www.instagram.com/p/";

    private static final Pattern PATTERN = Pattern.compile("window._sharedData = (.*);</script>");

    @Value("${integrations.instagram.pollUserName:juniperfoxx}")
    private String pollUserName;

    @Value("${integrations.instagram.ttl:30000}")
    private Long ttl;

    @Value("${integrations.instagram.updateInterval:30000}")
    private Long updateInterval;

    @Autowired
    private TaskScheduler scheduler;

    @Autowired
    private JuniPostRepository juniPostRepository;

    @Autowired
    private WebHookRepository webHookRepository;

    @Autowired
    private CommonProperties commonProperties;

    private InstagramProfile cache;

    private long latestUpdate;

    private RestTemplate restTemplate;

    private long latestId;

    @Value("${spring.application.name}")
    private String userName;

    @Getter
    @Value("${instagram.pollUserName:juniperfoxx}")
    private String accountName;

    @Getter
    private String iconUrl = "https://pbs.twimg.com/profile_images/913664565547638784/n31ZigvV.jpg";

    @PostConstruct
    public void init() {
        restTemplate = new RestTemplate(CommonUtils.createRequestFactory());
        scheduler.scheduleWithFixedDelay(this::update, updateInterval);
    }

    @Synchronized("$recentLock")
    public InstagramProfile getRecent() {
        try {
            long currentTimestamp = System.currentTimeMillis();
            if (currentTimestamp > latestUpdate + ttl) {
                ResponseEntity<String> response = restTemplate.getForEntity(ROOT_URL + pollUserName, String.class);
                if (HttpStatus.OK == response.getStatusCode()) {
                    String result = response.getBody();
                    if (result == null) {
                        return cache;
                    }

                    Matcher matcher = PATTERN.matcher(result);
                    if (matcher.find()) {
                        JsonParser parser = new JsonParser();
                        JsonObject data = parser.parse(matcher.group(1)).getAsJsonObject();

                        if (data != null) {
                            data = data.getAsJsonObject("entry_data");
                        }
                        if (data != null) {
                            JsonArray array = data.getAsJsonArray("ProfilePage");
                            if (array != null && array.size() > 0) {
                                data = array.get(0).getAsJsonObject();
                            }
                        }
                        if (data != null) {
                            data = data.getAsJsonObject("graphql");
                        }
                        if (data != null) {
                            data = data.getAsJsonObject("user");
                        }

                        InstagramProfile profile = new InstagramProfile();

                        if (data != null) {
                            profile.setFullName(data.get("full_name").getAsString());
                            profile.setImageUrl(data.get("profile_pic_url").getAsString());
                            data = data.getAsJsonObject("edge_owner_to_timeline_media");
                        }
                        if (data != null) {
                            JsonArray edges = data.getAsJsonArray("edges");
                            if (edges != null && edges.size() > 0) {
                                List<InstagramMedia> mediaList = new ArrayList<>();
                                profile.setFeed(mediaList);
                                for (JsonElement edge : edges) {
                                    JsonObject node = edge.getAsJsonObject().getAsJsonObject("node");
                                    if (node != null && node.get("id") != null) {
                                        InstagramMedia media = new InstagramMedia();
                                        media.setId(node.get("id").getAsLong());
                                        media.setImageUrl(node.get("display_url").getAsString());
                                        media.setText(getCaption(node.getAsJsonObject("edge_media_to_caption")));
                                        media.setDate(new Date(node.get("taken_at_timestamp").getAsLong() * 1000));
                                        media.setLink(POST_URL + node.get("shortcode").getAsString());
                                        mediaList.add(media);
                                    }
                                }
                                cache = profile;
                                latestUpdate = currentTimestamp;
                            }
                        }
                    }
                }
            }
        } catch (ResourceAccessException e) {
            // skip
        } catch (Exception e) {
            log.warn("Could not get Instagram data: {}", e.getMessage());
        }
        return cache;
    }

    private String getCaption(JsonObject root) {
        JsonArray edges = root.getAsJsonArray("edges");
        if (edges != null && edges.size() > 0) {
            JsonObject node = edges.get(0).getAsJsonObject();
            if (node != null) {
                node = node.getAsJsonObject("node");
            }
            if (node != null && node.get("text") != null) {
                return node.get("text").getAsString();
            }
        }
        return null;
    }

    private void update() {
        InstagramProfile profile = getRecent();
        if (profile == null || CollectionUtils.isEmpty(profile.getFeed())) {
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

            int size = Math.min(3, newMedias.size());
            if (size > 0) {
                List<WebhookEmbed> embeds = newMedias.stream()
                        .map(e -> convertToWebhookEmbed(profile, e).build())
                        .collect(Collectors.toList());

                WebhookMessage message = new WebhookMessageBuilder()
                        .setAvatarUrl(iconUrl)
                        .setUsername(accountName)
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

    public WebhookEmbedBuilder convertToWebhookEmbed(InstagramProfile profile, InstagramMedia media) {
        WebhookEmbedBuilder builder = new WebhookEmbedBuilder()
                .setImageUrl(media.getImageUrl())
                .setTimestamp(media.getDate().toInstant())
                .setColor(CommonUtils.hex2Rgb(commonProperties.getDefaultAccentColor()).getRGB())
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
