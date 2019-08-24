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
package ru.juniperbot.common.worker.configuration;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import ru.juniperbot.common.model.FeatureSet;

import java.util.*;

@Getter
@Setter
@Component
@ConfigurationProperties("juniperbot.worker")
public class WorkerProperties {

    private Discord discord = new Discord();

    private Audio audio = new Audio();

    private Stats stats = new Stats();

    private Events events = new Events();

    private Aiml aiml = new Aiml();

    private Audit audit = new Audit();

    private Patreon patreon = new Patreon();

    private DogAPI dogApi = new DogAPI();

    private Support support = new Support();

    @Getter
    @Setter
    @ToString
    public static class Discord {
        private int shardsTotal;
        private String token;
        private String playingStatus;
        private long reactionsTtlMs = 3600000;
    }

    @Getter
    @Setter
    @ToString
    public static class Stats {
        private String discordbotsOrgToken;
        private String discordbotsGgToken;
    }

    @Getter
    @Setter
    @ToString
    public static class Events {
        private boolean asyncExecution = true;
        private int corePoolSize = 5;
        private int maxPoolSize = 5;
    }

    @Getter
    @Setter
    @ToString
    public static class Aiml {
        private boolean enabled = true;
        private String brainsPath;
    }

    @Getter
    @Setter
    @ToString
    public static class Audit {
        private int keepMonths = 1;
        private boolean historyEnabled = true;
        private int historyDays = 7;
    }

    @Getter
    @Setter
    @ToString
    public static class Audio {

        private String resamplingQuality = "MEDIUM";
        private int frameBufferDuration = 2000;
        private int itemLoaderThreadPoolSize = 500;
        private int panelRefreshInterval = 5000;
        private Lavalink lavalink = new Lavalink();

        @Getter
        @Setter
        @ToString
        public static class Lavalink {

            private boolean enabled;
            private LavalinkDiscovery discovery = new LavalinkDiscovery();
            private List<LavalinkNode> nodes = new ArrayList<>();

            @Getter
            @Setter
            @ToString
            public static class LavalinkDiscovery {
                private boolean enabled;
                private String serviceName;
                private String password;
            }

            @Getter
            @Setter
            @ToString
            public static class LavalinkNode {
                private String name;
                private String url;
                private String password;
            }
        }
    }

    @Getter
    @Setter
    @ToString
    public static class Patreon {
        private String campaignId = "1552419";
        private String webhookSecret;
        private String accessToken;
        private String refreshToken;
        private boolean updateEnabled;
        private int updateInterval = 600000;
    }

    @Getter
    @Setter
    @ToString
    public static class DogAPI {
        private String userId;
        private String key;
    }

    @Getter
    @Setter
    @ToString
    public static class Support {
        private Long guildId;
        private Long donatorRoleId;
        private Long emergencyChannelId;
        private Map</* roleId*/ String, Set<FeatureSet>> featuredRoles = new HashMap<>();
    }
}