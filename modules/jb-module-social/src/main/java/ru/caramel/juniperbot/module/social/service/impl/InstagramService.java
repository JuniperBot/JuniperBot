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
package ru.caramel.juniperbot.module.social.service.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import ru.caramel.juniperbot.module.social.model.InstagramMedia;
import ru.caramel.juniperbot.module.social.model.InstagramProfile;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private PostService postService;

    private InstagramProfile cache;

    private long latestUpdate;

    private RestTemplate restTemplate;

    @PostConstruct
    public void init() {
        restTemplate = new RestTemplate();
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
        if (profile != null && CollectionUtils.isNotEmpty(profile.getFeed())) {
            postService.onInstagramUpdated(profile);
        }
    }
}
