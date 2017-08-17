package ru.caramel.juniperbot.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
public class YouTubeConfig {

    private String apiKey;

}
