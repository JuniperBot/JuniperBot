package ru.caramel.juniperbot.integration.youtube;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.caramel.juniperbot.configuration.YouTubeConfig;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Service
public class YouTubeClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(YouTubeClient.class);

    @Autowired
    private YouTubeConfig config;

    private YouTube youTube;

    @PostConstruct
    public void init() {
        try {
            youTube = new YouTube
                    .Builder(GoogleNetHttpTransport.newTrustedTransport(), JacksonFactory.getDefaultInstance(), e -> {})
                    .setApplicationName(YouTubeClient.class.getSimpleName())
                    .build();
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private List<SearchResult> search(String queryTerm)  {
        try {
            YouTube.Search.List search = youTube.search().list("id,snippet");
            search.setKey(config.getApiKey());
            search.setQ(queryTerm);
            search.setType("video");
            search.setFields("items(id/videoId)");
            search.setMaxResults(1L);
            return search.execute().getItems();
        } catch (IOException e) {
            LOGGER.error("Could not perform YouTube search", e);
        }
        return Collections.emptyList();
    }

    public String searchForUrl(String queryTerm) {
        List<SearchResult> result = search(queryTerm);
        if (!result.isEmpty()) {
            return String.format("https://www.youtube.com/watch?v=%s", result.get(0).getId().getVideoId());
        }
        return null;
    }
}
