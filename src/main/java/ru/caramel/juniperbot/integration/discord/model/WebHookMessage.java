package ru.caramel.juniperbot.integration.discord.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.impl.MessageEmbedImpl;
import org.apache.commons.collections4.CollectionUtils;
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
}
