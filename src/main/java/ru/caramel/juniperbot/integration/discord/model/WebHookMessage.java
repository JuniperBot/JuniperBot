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
package ru.caramel.juniperbot.integration.discord.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.impl.MessageEmbedImpl;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

@Getter
@Setter
@Builder
public class WebHookMessage {

    private String username;

    private String avatarUrl;

    private String content;

    private List<MessageEmbed> embeds;

    public JSONObject toJSONObject() {
        JSONObject obj = new JSONObject();
        if (username != null)
            obj.put("username", username);
        if (avatarUrl != null)
            obj.put("avatar_url", avatarUrl);
        if (content != null)
            obj.put("content", content);
        if (CollectionUtils.isNotEmpty(embeds)) {
            JSONArray array = new JSONArray();
            embeds.stream()
                    .map(e -> (MessageEmbedImpl) e)
                    .forEach(e -> array.put(e.toJSONObject()));
            obj.put("embeds", array);
        }
        return obj;
    }

    public boolean isEmpty() {
        return StringUtils.isEmpty(content) && CollectionUtils.isEmpty(embeds);
    }
}
