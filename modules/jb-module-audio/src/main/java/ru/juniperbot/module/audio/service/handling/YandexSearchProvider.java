/*
 * This file is part of JuniperBot.
 *
 * JuniperBot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * JuniperBot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with JuniperBot. If not, see <http://www.gnu.org/licenses/>.
 */
package ru.juniperbot.module.audio.service.handling;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import ru.juniperbot.module.audio.service.AudioSearchProvider;

import javax.annotation.PostConstruct;
import java.util.Map;

import static ru.juniperbot.common.utils.CommonUtils.HTTP_TIMEOUT_DURATION;

@Slf4j
@Component
public class YandexSearchProvider implements AudioSearchProvider {

    private static final String BASE_URI = "https://api.music.yandex.net/";
    private static final String TRACK_URL_FORMAT = "https://music.yandex.ru/album/%s/track/%s";

    private RestTemplate restTemplate;

    @PostConstruct
    private void init() {
        this.restTemplate = new RestTemplateBuilder()
                .rootUri(BASE_URI)
                .setConnectTimeout(HTTP_TIMEOUT_DURATION)
                .setReadTimeout(HTTP_TIMEOUT_DURATION)
                .uriTemplateHandler(new DefaultUriBuilderFactory(BASE_URI))
                .additionalInterceptors((request, body, execution) -> {
                    HttpHeaders headers = request.getHeaders();
                    headers.add("User-Agent", "Yandex-Music-API");
                    headers.add("X-Yandex-Music-Client", "WindowsPhone/3.20");
                    return execution.execute(request, body);
                })
                .build();
    }

    @Override
    public String searchTrack(String value) {
        if (StringUtils.isEmpty(value)) {
            return null;
        }
        String response = null;
        try {
            response = restTemplate.getForObject("search?type=track&page=0&text={text}", String.class, Map.of("text", value));
            if (response == null) {
                return null;
            }
            // Это дерьмище позорное, но я не хочу ради одного только поиска писать кучу классов-врапперов под это.
            // Мне оно не нужно, работает и ладно. х)
            JsonObject json = new JsonParser().parse(response).getAsJsonObject();
            json = json.getAsJsonObject("result");
            if (json == null) {
                return null;
            }
            json = json.getAsJsonObject("tracks");
            if (json == null) {
                return null;
            }
            JsonArray results = json.getAsJsonArray("results");
            if (results == null || results.size() == 0) {
                return null;
            }
            json = results.get(0).getAsJsonObject();
            String trackId = json.get("id").getAsString();
            if (StringUtils.isEmpty(trackId)) {
                return null;
            }
            results = json.getAsJsonArray("albums");
            if (results == null || results.size() == 0) {
                return null;
            }
            json = results.get(0).getAsJsonObject();
            String albumId = json.get("id").getAsString();
            return String.format(TRACK_URL_FORMAT, albumId, trackId);
        } catch (Exception e) {
            log.warn("Could not search Yandex, response:\n{}", response, e);
            return null;
        }
    }

    @Override
    public String getProviderName() {
        return "yandex";
    }
}
