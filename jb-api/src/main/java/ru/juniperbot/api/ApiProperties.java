/*
 * This file is part of JuniperBot.
 *
 * JuniperBot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * JuniperBot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with JuniperBot. If not, see <http://www.gnu.org/licenses/>.
 */
package ru.juniperbot.api;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties("juniperbot.api")
public class ApiProperties {

    private Discord discord = new Discord();

    private Cors cors = new Cors();

    private Blur blur = new Blur();

    private Twitch twitch = new Twitch();

    private YouTube youTube = new YouTube();

    @Getter
    @Setter
    @ToString
    public static class Cors {
        private List<String> allowedMethods = Arrays.asList("GET", "HEAD", "POST", "OPTIONS", "PUT", "DELETE");
        private List<String> privateOrigins;
    }

    @Getter
    @Setter
    @ToString
    public static class Discord {
        private String clientId;
        private String clientSecret;
        private String authorizeUri;
        private String tokenUri;
        private String userInfoUri;
        private String guildsInfoUri;
        private String scope;
    }

    @Getter
    @Setter
    @ToString
    public static class Blur {
        private boolean useOpenCV = true;
        private int radius = 201;
        private String cachePath;
    }

    @Getter
    @Setter
    @ToString
    public static class Twitch {
        private String clientId;
        private String secret;
        private String oauthKey;
        private Long updateInterval;
    }

    @Getter
    @Setter
    @ToString
    public static class YouTube {
        private String pubSubSecret;
        private int resubscribeThresholdPct = 10;
    }
}