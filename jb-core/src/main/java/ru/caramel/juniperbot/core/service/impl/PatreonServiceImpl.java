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

import com.patreon.PatreonAPI;
import com.patreon.PatreonOAuth;
import com.patreon.resources.Pledge;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import ru.caramel.juniperbot.core.feature.BaseOwnerFeatureSetProvider;
import ru.caramel.juniperbot.core.model.FeatureProvider;
import ru.caramel.juniperbot.core.model.enums.FeatureSet;
import ru.caramel.juniperbot.core.persistence.entity.PatreonUser;
import ru.caramel.juniperbot.core.persistence.repository.PatreonUserRepository;
import ru.caramel.juniperbot.core.service.EmergencyService;
import ru.caramel.juniperbot.core.service.PatreonService;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@FeatureProvider(priority = 2)
public class PatreonServiceImpl extends BaseOwnerFeatureSetProvider implements PatreonService {

    private static final Logger log = LoggerFactory.getLogger(PatreonServiceImpl.class);

    @Value("${integrations.patreon.campaignId:1552419}")
    private String campaignId;

    @Value("${integrations.patreon.clientId:}")
    private String clientId;

    @Value("${integrations.patreon.clientSecret:}")
    private String clientSecret;

    @Value("${integrations.patreon.accessToken:}")
    private String accessToken;

    @Value("${integrations.patreon.refreshToken:}")
    private String refreshToken;

    @Value("${integrations.patreon.redirectUri:}")
    private String redirectUri;

    @Value("${integrations.patreon.updateInterval:3600000}")
    private Long updateInterval;

    @Autowired
    private EmergencyService emergencyService;

    @Autowired
    private PatreonUserRepository repository;

    @Autowired
    private TaskScheduler scheduler;

    private PatreonOAuth oAuth;

    private PatreonAPI creatorApi;

    private final Map<Long, Set<FeatureSet>> featureSets = new ConcurrentHashMap<>();

    @PostConstruct
    private void init() {
        if (StringUtils.isEmpty(clientId)
                || StringUtils.isEmpty(clientSecret)
                || StringUtils.isEmpty(redirectUri)
                || StringUtils.isEmpty(accessToken)) {
            log.warn("No Patreon credentials specified, integration would not work");
            return;
        }
        oAuth = new PatreonOAuth(clientId, clientSecret, redirectUri);
        creatorApi = new PatreonAPI(accessToken);
        List<PatreonUser> activeUsers = repository.findActive();
        featureSets.putAll(activeUsers.stream().collect(Collectors.toMap(e -> Long.valueOf(e.getUserId()), PatreonUser::getFeatureSets)));
        scheduler.scheduleWithFixedDelay(this::update, updateInterval);
    }

    private synchronized void update() {
        log.info("Starting Patreon pledges fetching");
        try {
            List<PatreonUser> patreonUsers = repository.findAll();
            Map<String, PatreonUser> patreonById = patreonUsers.stream().collect(Collectors.toMap(PatreonUser::getPatreonId, e -> e));
            Map<String, PatreonUser> patreonByUserId = patreonUsers.stream().collect(Collectors.toMap(PatreonUser::getPatreonId, e -> e));

            patreonUsers.forEach(e -> e.setActive(false));
            Map<Long, Set<FeatureSet>> featureSets = new HashMap<>();

            List<Pledge> pledges = creatorApi.fetchAllPledges(campaignId);
            for (Pledge pledge : pledges) {
                if (pledge.getPatron() == null) {
                    log.warn("No patron found for Pledge {}", pledge.getId());
                    continue;
                }

                String discordUserId = null;
                if (pledge.getPatron().getSocialConnections() != null
                        && pledge.getPatron().getSocialConnections().getDiscord() != null
                        && StringUtils.isNotEmpty(pledge.getPatron().getSocialConnections().getDiscord().getUser_id())) {
                    discordUserId = pledge.getPatron().getSocialConnections().getDiscord().getUser_id();
                }

                PatreonUser patreon = patreonById.get(pledge.getPatron().getId());

                if (patreon == null) {
                    if (discordUserId != null) {
                        patreon = patreonByUserId.get(discordUserId);
                    } else {
                        continue; // we could not assign user at least now
                    }
                }

                if (patreon == null) {
                    patreon = new PatreonUser();
                    patreonUsers.add(patreon);
                }

                patreon.setPatreonId(pledge.getPatron().getId());
                if (discordUserId != null) {
                    patreon.setUserId(discordUserId);
                }
                patreon.setActive(true);

                Set<FeatureSet> pledgeSets = new HashSet<>();
                if (pledge.getAmountCents() >= 100) {
                    pledgeSets.add(FeatureSet.BONUS);
                }
                patreon.setFeatureSets(pledgeSets);

                featureSets.put(Long.valueOf(patreon.getUserId()), patreon.getFeatureSets());
            }

            this.featureSets.clear();
            this.featureSets.putAll(featureSets);
            repository.saveAll(patreonUsers);
            repository.flush();
        } catch (Exception e) {
            log.error("Could not update Patreon Pledges", e);
            emergencyService.error("Could not update Patreon Pledges", e);
        }
        log.info("Finished Patreon pledges fetching");
    }

    @Override
    public Set<FeatureSet> getByUser(long userId) {
        return featureSets.getOrDefault(userId, Set.of());
    }
}
