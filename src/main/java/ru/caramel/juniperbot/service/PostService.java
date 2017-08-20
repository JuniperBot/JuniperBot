package ru.caramel.juniperbot.service;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageChannel;
import org.jinstagram.entity.users.feed.MediaFeedData;

import java.util.List;

public interface PostService {

    void post(List<MediaFeedData> medias, MessageChannel channel);

    EmbedBuilder convertToEmbed(MediaFeedData media);
}
