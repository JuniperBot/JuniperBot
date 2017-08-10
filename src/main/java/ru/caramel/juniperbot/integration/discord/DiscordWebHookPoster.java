package ru.caramel.juniperbot.integration.discord;

import lombok.Setter;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import org.jinstagram.entity.users.feed.MediaFeedData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.caramel.juniperbot.commands.phyr.PostCommand;
import ru.caramel.juniperbot.configuration.DiscordConfig;
import ru.caramel.juniperbot.integration.instagram.InstagramListener;
import ru.caramel.juniperbot.integration.discord.model.WebHookMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DiscordWebHookPoster implements InstagramListener {

    @Autowired
    private DiscordConfig config;

    @Autowired
    private DiscordClient client;

    @Setter
    private boolean resetToSecond;

    private String latestId;

    @Override
    public void onInstagramUpdated(List<MediaFeedData> medias) {
        synchronized (this) {
            if (resetToSecond) {
                if (medias.size() > 1) {
                    latestId = medias.get(1).getId();
                }
                resetToSecond = false;
            }
        }

        if (latestId != null) {
            List<MediaFeedData> newMedias = new ArrayList<>();
            for (MediaFeedData media : medias) {
                if (media.getId().equals(latestId)) {
                    break;
                }
                newMedias.add(media);
            }

            int size = Math.min(PostCommand.MAX_DETAILED, newMedias.size());
            if (size > 0) {
                List<MessageEmbed> embeds = newMedias.stream()
                        .map(e -> {
                            EmbedBuilder builder = PostCommand.convertToEmbed(e);
                            builder.setColor(config.getAccentColor());
                            return builder.build();
                        }).collect(Collectors.toList());

                WebHookMessage message = WebHookMessage.builder()
                        .avatarUrl(config.getAvatarUrl())
                        .username(config.getUserName())
                        .embeds(embeds)
                        .build();

                config.getWebHooks().forEach(e -> client.executeWebHook(e, message));
            }
        }
        latestId = medias.get(0).getId();
    }
}
