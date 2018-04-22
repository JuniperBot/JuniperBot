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
package ru.caramel.juniperbot.module.ranking.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import ru.caramel.juniperbot.module.ranking.model.RankingInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.function.Consumer;

@Component
public class ImportProvider {

    private final static String apiEndpoint = new String(Base64.getDecoder().decode("aHR0cHM6Ly9hcGkubWVlNi54eXovcGx1Z2lucy9sZXZlbHMvbGVhZGVyYm9hcmQve2d1aWxkSWR9P3BhZ2U9e3BhZ2VOdW19"));

    private final static String AVATAR_FORMAT = "https://cdn.discordapp.com/avatars/%s/%s.jpg";

    private OkHttpClient client = new OkHttpClient.Builder().build();

    public void export(long guildId, Consumer<List<RankingInfo>> supplier) {
        int page = 0;
        while (true) {
            Request request = new Request.Builder()
                    .url(apiEndpoint.replace("{guildId}", String.valueOf(guildId)).replace("{pageNum}",
                            String.valueOf(page)))
                    .build();

            try (Response response = client.newCall(request).execute()) {
                JsonParser parser = new JsonParser();
                JsonObject data = parser.parse(response.body().string()).getAsJsonObject();
                JsonArray players = data.getAsJsonArray("players");
                if (players == null || players.size() == 0) {
                    break;
                }

                List<RankingInfo> batch = new ArrayList<>(players.size());
                for (JsonElement player : players) {
                    JsonObject object = player.getAsJsonObject();
                    RankingInfo rankingInfo = new RankingInfo();
                    rankingInfo.setId(getString(object, "id"));
                    rankingInfo.setDiscriminator(getString(object, "discriminator"));
                    rankingInfo.setName(getString(object, "username"));
                    rankingInfo.setNick(getString(object, "username"));
                    String avatar = getString(object, "id");
                    if (StringUtils.isNotEmpty(avatar)) {
                        rankingInfo.setAvatarUrl(String.format(AVATAR_FORMAT, rankingInfo.getId(), avatar));
                    }
                    rankingInfo.setTotalExp(object.get("xp").getAsLong());
                    batch.add(rankingInfo);
                }
                supplier.accept(batch);
                page++;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private String getString(JsonObject object, String name) {
        JsonElement element = object.get(name);
        return element != null && !element.isJsonNull() ? element.getAsString() : null;
    }
}
