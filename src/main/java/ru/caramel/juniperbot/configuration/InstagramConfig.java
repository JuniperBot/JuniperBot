package ru.caramel.juniperbot.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
public class InstagramConfig {

    private String clientId;

    private String clientSecret;

    private String token;

    private String pollUserId;

    private Long ttl;

    private Long updateInterval;

    public Long getTtl() {
        return ttl != null ? ttl : 30000;
    }

    public Long getUpdateInterval() {
        return updateInterval != null ? updateInterval : 30000;
    }
}
