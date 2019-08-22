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
package ru.juniperbot.common.configuration;

import com.rabbitmq.client.ConnectionFactory;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@Component
@ConfigurationProperties("juniperbot.common")
public class CommonProperties {

    private Jmx jmx = new Jmx();

    private Discord discord = new Discord();

    private Execution execution = new Execution();

    private RabbitMQ rabbitMQ = new RabbitMQ();

    private Branding branding = new Branding();

    private DomainCache domainCache = new DomainCache();

    private List<String> youTubeApiKeys = new ArrayList<>();

    @Getter
    @Setter
    @ToString
    public static class Jmx {
        private boolean enabled;
        private int port = 9875;
    }

    @Getter
    @Setter
    @ToString
    public static class Discord {
        private String defaultPrefix = "!";
        private String defaultAccentColor = "#FFA550";
        private String superUserId;
    }

    @Getter
    @Setter
    @ToString
    public static class Execution {
        private int corePoolSize = 5;
        private int maxPoolSize = 5;
        private int schedulerPoolSize = 10;
    }

    @Getter
    @Setter
    @ToString
    public static class RabbitMQ {
        private String hostname = "localhost";
        private int port = ConnectionFactory.DEFAULT_AMQP_PORT;
        private String username;
        private String password;
    }

    @Getter
    @Setter
    @ToString
    public static class Branding {
        private String avatarUrl;
        private String avatarSmallUrl;
        private String copyrightIconUrl;
        private String websiteUrl;
        private Set<String> websiteAliases;
    }

    @Getter
    @Setter
    @ToString
    public static class DomainCache {
        private boolean auditConfig = true;
        private boolean guildConfig = true;
        private boolean moderationConfig = true;
        private boolean musicConfig = true;
        private boolean rankingConfig = true;
        private boolean reactionRouletteConfig = true;
        private boolean welcomeConfig = true;
    }
}