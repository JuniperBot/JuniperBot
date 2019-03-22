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
package ru.caramel.juniperbot.core.patreon.service;

import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.transaction.annotation.Transactional;
import ru.caramel.juniperbot.core.configuration.SchedulerConfiguration;
import ru.caramel.juniperbot.core.feature.provider.BaseOwnerFeatureSetProvider;
import ru.caramel.juniperbot.core.feature.model.FeatureProvider;
import ru.caramel.juniperbot.core.patreon.model.Member;
import ru.caramel.juniperbot.core.feature.model.FeatureSet;
import ru.caramel.juniperbot.core.patreon.model.User;
import ru.caramel.juniperbot.core.patreon.persistence.PatreonUser;
import ru.caramel.juniperbot.core.patreon.persistence.PatreonUserRepository;
import ru.caramel.juniperbot.core.common.service.EmergencyService;
import ru.caramel.juniperbot.core.patreon.PatreonAPI;
import ru.caramel.juniperbot.core.utils.PatreonUtils;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

@Slf4j
@FeatureProvider(priority = 2)
public class PatreonServiceImpl extends BaseOwnerFeatureSetProvider implements PatreonService {

    private final Object $lock = new Object[0];

    private final Object $webHookLock = new Object[0];

    @Value("${integrations.patreon.campaignId:1552419}")
    private String campaignId;

    @Value("${integrations.patreon.webhookSecret:}")
    private String webHookSecret;

    @Value("${integrations.patreon.accessToken:}")
    private String accessToken;

    @Value("${integrations.patreon.refreshToken:}")
    private String refreshToken;

    @Value("${integrations.patreon.updateEnabled:false}")
    private boolean updateEnabled;

    @Value("${integrations.patreon.updateInterval:3600000}")
    private Long updateInterval;

    @Autowired
    private EmergencyService emergencyService;

    @Autowired
    private PatreonUserRepository repository;

    @Autowired
    @Qualifier(SchedulerConfiguration.COMMON_SCHEDULER_NAME)
    private TaskScheduler scheduler;

    private PatreonAPI creatorApi;

    private HmacUtils webHookHmac;

    private ScheduledFuture<?> updateFuture;

    private final Map<Long, Set<FeatureSet>> featureSets = new ConcurrentHashMap<>();

    @PostConstruct
    private void init() {
        List<PatreonUser> activeUsers = repository.findActive();
        featureSets.putAll(activeUsers.stream().collect(Collectors.toMap(e -> Long.valueOf(e.getUserId()), PatreonUser::getFeatureSets)));

        if (StringUtils.isNotEmpty(accessToken)) {
            creatorApi = new PatreonAPI(accessToken);
            if (updateEnabled) {
                updateFuture = scheduler.scheduleWithFixedDelay(this::update, updateInterval);
            }
        } else {
            log.warn("No Patreon credentials specified, integration would not work");
        }
        if (StringUtils.isNotEmpty(webHookSecret)) {
            webHookHmac = new HmacUtils(HmacAlgorithms.HMAC_MD5, webHookSecret);
        } else {
            log.warn("No Patreon WebHook secret specified, WebHooks would not work");
        }
    }

    @Override
    @Transactional
    public synchronized void update() {
        log.info("Starting Patreon pledges fetching");
        try {
            List<PatreonUser> patreonUsers = repository.findAll();
            Map<String, PatreonUser> patreonById = patreonUsers.stream().collect(Collectors.toMap(PatreonUser::getPatreonId, e -> e));
            Map<String, PatreonUser> patreonByUserId = patreonUsers.stream().collect(Collectors.toMap(PatreonUser::getUserId, e -> e));

            patreonUsers.forEach(e -> e.setActive(false));
            Map<Long, Set<FeatureSet>> featureSets = new HashMap<>();

            List<Member> members = creatorApi.fetchAllMembers(campaignId);
            for (Member member : members) {
                User patron = member.getUser();
                if (member.getUser() == null) {
                    log.warn("No patron found for Pledge {}", member.getUser());
                    continue;
                }

                String discordUserId = getDiscordId(patron);

                PatreonUser patreon = patreonById.get(patron.getId());

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

                patreon.setPatreonId(patron.getId());
                if (discordUserId != null) {
                    patreon.setUserId(discordUserId);
                }
                patreon.setActive(member.isActiveAndPaid());
                patreon.setFeatureSets(getFeatureSets(member));

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

    @Override
    @Transactional
    @Synchronized("$webHookLock")
    public boolean processWebHook(String content, String trigger, String signature) {
        try {
            log.info("Incoming Patreon WebHook [{}]: {}", trigger, content);
            if (signature != null && webHookHmac != null && !signature.equalsIgnoreCase(webHookHmac.hmacHex(content))) {
                log.warn("Denied Patreon WebHook!");
                return false;
            }

            Member member = PatreonUtils.parseMember(content);
            PatreonUser patron = getOrCreatePatron(member);
            if (patron == null) {
                return true; // treat it is as success, we could not find such user yet
            }

            switch (trigger) {
                case "members:pledge:create":
                case "members:pledge:update":
                    patron.setActive(true);
                    Set<FeatureSet> entitledFeatureSets = getFeatureSets(member);
                    patron.setFeatureSets(entitledFeatureSets);
                    featureSets.put(Long.valueOf(patron.getUserId()), entitledFeatureSets);
                    break;
                case "members:pledge:delete":
                    patron.setActive(false);
                    featureSets.remove(Long.valueOf(patron.getUserId()));
                    break;
            }
            repository.save(patron);
        } catch (Exception e) {
            log.error("Could not perform Patreon WebHook [event={}]: {}", trigger, content, e);
            emergencyService.error("Could not perform Patreon WebHook", e);
        }
        return true;
    }

    private PatreonUser getOrCreatePatron(Member member) {
        User user = member.getUser();
        if (user == null) {
            return null;
        }

        String discordUserId = getDiscordId(user);
        PatreonUser patron = repository.findByPatreonId(user.getId());
        if (patron == null && discordUserId == null) {
            return null;
        }
        if (patron == null) {
            patron = repository.findByUserId(discordUserId);
        }
        if (patron == null) {
            patron = new PatreonUser();
        }
        patron.setPatreonId(user.getId());
        if (discordUserId != null) {
            patron.setUserId(discordUserId);
        }
        return patron;
    }

    @Override
    public void setUpdateEnabled(boolean enabled) {
        this.updateEnabled = enabled;
        if (enabled && updateFuture == null) {
            synchronized ($lock) {
                if (updateFuture == null) {
                    updateFuture = scheduler.scheduleWithFixedDelay(this::update, updateInterval);
                }
            }
        } else if (!enabled && updateFuture != null) {
            synchronized ($lock) {
                if (updateFuture != null) {
                    updateFuture.cancel(false);
                    updateFuture = null;
                }
            }
        }
    }

    private static String getDiscordId(User user) {
        return user != null && user.getSocialConnections() != null
                && user.getSocialConnections().getDiscord() != null
                && StringUtils.isNotEmpty(user.getSocialConnections().getDiscord().getUser_id())
                ? user.getSocialConnections().getDiscord().getUser_id() : null;
    }

    private static Set<FeatureSet> getFeatureSets(Member member) {
        Set<FeatureSet> pledgeSets = new HashSet<>();
        if (member.isActiveAndPaid()) {
            Integer cents = member.getCurrentlyEntitledAmountCents();
            if (member.getCurrentlyEntitledAmountCents() != null) {
                if (cents >= 100) {
                    pledgeSets.add(FeatureSet.BONUS);
                }
            }
        }
        return pledgeSets;
    }
}
