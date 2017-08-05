package ru.caramel.juniperbot.integration.instagram;

import org.jinstagram.entity.users.feed.MediaFeedData;

import java.util.List;

public interface InstagramListener {

    void onInstagramUpdated(List<MediaFeedData> medias);
}
