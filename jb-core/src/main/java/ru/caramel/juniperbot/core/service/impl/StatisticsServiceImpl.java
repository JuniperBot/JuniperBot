/*
 * This file is part of JuniperBotJ.
 *
 * JuniperBotJ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * JuniperBotJ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with JuniperBotJ. If not, see <http://www.gnu.org/licenses/>.
 */
package ru.caramel.juniperbot.core.service.impl;

import lombok.Getter;
import net.dv8tion.jda.core.JDA;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.caramel.juniperbot.core.model.ProviderStats;
import ru.caramel.juniperbot.core.service.StatisticsService;

import java.util.concurrent.atomic.AtomicLong;

@Service
public class StatisticsServiceImpl implements StatisticsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StatisticsServiceImpl.class);

    private static final String ORG_ENDPOINT = "https://discordbots.org/api/bots/{clientId}/stats";

    private static final String PW_ENDPOINT = "https://bots.discord.pw/api/bots/{clientId}/stats";

    private RestTemplate restTemplate = new RestTemplate();

    @Getter
    private AtomicLong serverCount = new AtomicLong();

    @Value("${discord.oauth.clientId}")
    private String clientId;

    @Value("${discord.stats.discordbots.org.token:}")
    private String orgToken;

    @Value("${discord.stats.bots.discord.pw.token:}")
    private String pwToken;

    @Override
    public void notifyProviders(JDA shard) {
        ProviderStats stats = new ProviderStats(shard);
        notifyProvider(stats, ORG_ENDPOINT, orgToken);
        notifyProvider(stats, PW_ENDPOINT, pwToken);
    }

    private void notifyProvider(ProviderStats stats, String endPoint, String token) {
        if (StringUtils.isEmpty(token)) {
            return;
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.add("Authorization", token);
            HttpEntity<ProviderStats> request = new HttpEntity<>(stats, headers);
            ResponseEntity<String> response = restTemplate.exchange(endPoint, HttpMethod.POST, request, String.class, clientId);
            if (!HttpStatus.OK.equals(response.getStatusCode())) {
                LOGGER.warn("Could not report stats {} to endpoint {}: response is {}", stats, endPoint,
                        response.getStatusCode());
            }
        } catch (Exception e) {
            LOGGER.warn("Could not report stats {} to endpoint {}", stats, endPoint, e);
        }
    }
}
