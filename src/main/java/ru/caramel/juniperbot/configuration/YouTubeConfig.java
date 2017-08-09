package ru.caramel.juniperbot.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

@Component
public class YouTubeConfig {

    @Getter
    @Setter
    private String apiKey;

}
