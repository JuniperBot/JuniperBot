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
package ru.juniperbot.common.worker.patreon.service;

import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.transaction.annotation.Transactional;
import ru.juniperbot.common.configuration.CommonConfiguration;
import ru.juniperbot.common.model.FeatureSet;
import ru.juniperbot.common.model.patreon.Member;
import ru.juniperbot.common.model.patreon.User;
import ru.juniperbot.common.model.request.PatreonRequest;
import ru.juniperbot.common.persistence.entity.PatreonUser;
import ru.juniperbot.common.persistence.repository.PatreonUserRepository;
import ru.juniperbot.common.utils.PatreonUtils;
import ru.juniperbot.common.worker.configuration.WorkerProperties;
import ru.juniperbot.common.worker.feature.provider.BaseOwnerFeatureSetProvider;
import ru.juniperbot.common.worker.feature.provider.FeatureProvider;
import ru.juniperbot.common.worker.patreon.PatreonAPI;
import ru.juniperbot.common.worker.shared.service.EmergencyService;
import ru.juniperbot.common.worker.shared.service.SupportService;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@FeatureProvider(priority = 3)
public class PatreonServiceImpl extends BaseOwnerFeatureSetProvider implements PatreonService {

    private final Object $lock = new Object[0];

    private final Object $webHookLock = new Object[0];

    private final Object $boostLock = new Object[0];

    @Autowired
    private WorkerProperties workerProperties;

    @Autowired
    private EmergencyService emergencyService;

    @Autowired
    private PatreonUserRepository repository;

    @Autowired
    private SupportService supportService;

    @Autowired
    @Qualifier(CommonConfiguration.SCHEDULER)
    private TaskScheduler scheduler;

    private PatreonAPI creatorApi;

    private HmacUtils webHookHmac;

    private final Map<Long, Set<FeatureSet>> featureSets = new ConcurrentHashMap<>();

    private final Map</* Boosted Guild ID */Long, /* User IDs */Set<Long>> boostedGuilds = new ConcurrentHashMap<>();

    @PostConstruct
    private void init() {
        List<PatreonUser> activeUsers = repository.findActive();
        activeUsers.forEach(this::enableFeatures);

        String accessToken = workerProperties.getPatreon().getAccessToken();
        if (StringUtils.isNotEmpty(accessToken)) {
            creatorApi = new PatreonAPI(accessToken);
            if (workerProperties.getPatreon().isUpdateEnabled()) {
                scheduler.scheduleWithFixedDelay(this::update, workerProperties.getPatreon().getUpdateInterval());
            }
        } else {
            log.warn("No Patreon credentials specified, integration would not work");
        }
        String webhookSecret = workerProperties.getPatreon().getWebhookSecret();
        if (StringUtils.isNotEmpty(webhookSecret)) {
            webHookHmac = new HmacUtils(HmacAlgorithms.HMAC_MD5, webhookSecret);
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

            patreonUsers.forEach(e -> {
                e.setActive(false);
                e.setFeatures(null);
            });

            Set<String> donators = new HashSet<>();

            List<Member> members = creatorApi.fetchAllMembers(workerProperties.getPatreon().getCampaignId());
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
                    patreon.setPatreonId(patron.getId());
                    patreonUsers.add(patreon);
                    patreonById.put(patron.getId(), patreon);
                }

                if (discordUserId != null) {
                    patreon.setUserId(discordUserId);
                    patreonByUserId.put(discordUserId, patreon);
                }

                if (member.isActiveAndPaid()) {
                    patreon.setActive(true);
                    patreon.appendFeatureSets(getFeatureSets(member));
                    if (discordUserId != null) {
                        donators.add(discordUserId);
                    }
                }
            }

            this.featureSets.clear();
            this.boostedGuilds.clear();
            patreonUsers.forEach(this::enableFeatures);
            repository.saveAll(patreonUsers);
            repository.flush();
            supportService.grantDonators(donators);
        } catch (Throwable e) {
            log.error("Could not update Patreon Pledges", e);
            emergencyService.error("Could not update Patreon Pledges", e);
        }
        log.info("Finished Patreon pledges fetching");
    }

    @Override
    @Transactional
    @Synchronized("$boostLock")
    public boolean tryBoost(long userId, long guildId) {
        PatreonUser user = repository.findByUserId(String.valueOf(userId));
        if (user == null || !user.isActive() || CollectionUtils.isEmpty(user.getFeatureSets())) {
            return false;
        }

        // unboost previous guild
        if (user.getBoostedGuildId() != null) {
            Set<Long> boostedUsers = this.boostedGuilds.get(user.getBoostedGuildId());
            if (boostedUsers != null) {
                boostedUsers.remove(userId);
            }
        }
        // boost new guild
        boostedGuilds.computeIfAbsent(guildId, e -> new HashSet<>()).add(userId);
        user.setBoostedGuildId(guildId);
        repository.save(user);
        return true;
    }

    @Override
    @Transactional
    @Synchronized("$boostLock")
    public boolean removeBoost(long userId, long guildId) {
        PatreonUser user = repository.findByUserId(String.valueOf(userId));
        if (user == null
                || user.getBoostedGuildId() == null
                || CollectionUtils.isEmpty(user.getFeatureSets())) {
            return false;
        }
        Set<Long> boostedUsers = this.boostedGuilds.get(user.getBoostedGuildId());
        if (boostedUsers != null) {
            boostedUsers.remove(userId);
        }
        user.setBoostedGuildId(null);
        repository.save(user);
        return true;
    }

    @Override
    public Set<FeatureSet> getByUser(long userId) {
        return featureSets.getOrDefault(userId, Set.of());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isAvailable(long guildId, FeatureSet featureSet) {
        if (super.isAvailable(guildId, featureSet)) {
            return true;
        }
        return this.boostedGuilds.getOrDefault(guildId, Set.of()).stream().anyMatch(e -> isAvailableForUser(e, featureSet));
    }

    @Override
    @Transactional(readOnly = true)
    public Set<FeatureSet> getByGuild(long guildId) {
        Set<FeatureSet> result = new HashSet<>(super.getByGuild(guildId));
        this.boostedGuilds.getOrDefault(guildId, Set.of()).forEach(e -> result.addAll(getByUser(e)));
        return result;
    }

    @Override
    @Transactional
    @Synchronized("$webHookLock")
    public boolean processWebHook(PatreonRequest request) {
        try {
            log.info("Incoming Patreon WebHook {}", request);
            String content = request.getContent();
            String signature = request.getSignature();
            String event = request.getEvent();
            if (signature != null && webHookHmac != null && !signature.equalsIgnoreCase(webHookHmac.hmacHex(content))) {
                log.warn("Denied Patreon WebHook!");
                return false;
            }

            Member member = PatreonUtils.parseMember(content);
            PatreonUser patron = getOrCreatePatron(member);
            if (patron == null) {
                log.warn("No such Discord user found for incoming WebHook");
                return true; // treat it is as success, we could not find such user yet
            }

            switch (event) {
                case "members:pledge:create":
                case "members:pledge:update":
                    patron.setActive(true);
                    Set<FeatureSet> entitledFeatureSets = getFeatureSets(member);
                    patron.setFeatureSets(entitledFeatureSets);
                    enableFeatures(patron);
                    if (CollectionUtils.isNotEmpty(entitledFeatureSets)) {
                        supportService.grantDonators(Collections.singleton(patron.getUserId()));
                    }
                    log.info("Updated user {} features: {}", patron.getUserId(), entitledFeatureSets);
                    break;
                case "members:pledge:delete":
                    patron.setActive(false);
                    disableFeatures(patron);
                    break;
            }
            repository.save(patron);
        } catch (Exception e) {
            log.error("Could not perform Patreon WebHook [{}]: {}", request, e);
            emergencyService.error("Could not perform Patreon WebHook", e);
        }
        return true;
    }

    private void enableFeatures(PatreonUser user) {
        if (!user.isActive()) {
            return;
        }
        Long userId = Long.valueOf(user.getUserId());
        featureSets.put(userId, user.getFeatureSets());

        if (user.getBoostedGuildId() != null) {
            boostedGuilds.computeIfAbsent(user.getBoostedGuildId(), e -> new HashSet<>()).add(userId);
        }
    }

    private void disableFeatures(PatreonUser user) {
        Long userId = Long.valueOf(user.getUserId());
        featureSets.remove(userId);
        if (user.getBoostedGuildId() != null) {
            boostedGuilds.computeIfAbsent(user.getBoostedGuildId(), e -> new HashSet<>()).remove(userId);
        }
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
            if (cents != null && cents >= 200) {
                pledgeSets.add(FeatureSet.BONUS);
            }
        }
        return pledgeSets;
    }
}
