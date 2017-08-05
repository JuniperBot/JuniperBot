package ru.caramel.juniperbot.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

@Component
public class InstagramConfig {

    @Getter
    @Setter
    private String clientId;

    @Getter
    @Setter
    private String clientSecret;

    @Getter
    @Setter
    private String token;

    @Getter
    @Setter
    private String pollUserId;

    @Setter
    private Long ttl;

    @Setter
    private Long updateInterval;

    public Long getTtl() {
        return ttl != null ? ttl : 30000;
    }

    public Long getUpdateInterval() {
        return updateInterval != null ? updateInterval : 30000;
    }
}
