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
package ru.caramel.juniperbot.module.steam.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import ru.caramel.juniperbot.module.steam.model.GetAppListResponse;
import ru.caramel.juniperbot.module.steam.model.details.SteamAppDetails;
import ru.caramel.juniperbot.module.steam.model.SteamAppEntry;
import ru.caramel.juniperbot.module.steam.persistence.entity.SteamApp;
import ru.caramel.juniperbot.module.steam.persistence.entity.SteamCache;
import ru.caramel.juniperbot.module.steam.persistence.repository.SteamAppRepository;
import ru.caramel.juniperbot.module.steam.persistence.repository.SteamCacheRepository;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class SteamServiceImpl implements SteamService {

    private static final ObjectMapper mapper = new ObjectMapper();

    private static final String APPS_ENDPOINT = "https://api.steampowered.com/ISteamApps/GetAppList/v2/";

    private static final String DETAILS_ENDPOINT = "https://store.steampowered.com/api/appdetails?appids=%s&l=%s";

    @Autowired
    private SteamAppRepository appRepository;

    @Autowired
    private SteamCacheRepository cacheRepository;

    private RestTemplate restTemplate = new RestTemplate();

    @PostConstruct
    public void init() {
        if (appRepository.count() == 0) {
            try {
                rebuildApps();
            } catch (Exception e) {
                log.error("Cannot initiate app repository", e);
            }
        }
    }

    @Scheduled(cron="0 0 0 * * ?")
    @Transactional
    @Override
    public void rebuildApps() {
        ResponseEntity<GetAppListResponse> response = restTemplate.getForEntity(APPS_ENDPOINT, GetAppListResponse.class);
        if (!HttpStatus.OK.equals(response.getStatusCode())) {
            log.warn("Could not get app list, endpoint returned {}", response.getStatusCode());
        }
        if (response.getBody() == null) {
            log.warn("Empty Apps list returned");
            return;
        }
        SteamAppEntry[] apps = response.getBody().getApps();
        long count = appRepository.count();
        if (apps != null && apps.length != count) {
            Map<Long, String> newMap = Stream.of(apps).collect(Collectors.toMap(SteamAppEntry::getAppid, SteamAppEntry::getName));
            Set<Long> existentApps = appRepository.findAllIds();

            // Apps to add
            Set<Long> idsToAdd = new HashSet<>(newMap.keySet());
            idsToAdd.removeAll(existentApps);

            if (!idsToAdd.isEmpty()) {
                List<SteamApp> appsToAdd = idsToAdd.stream().map(e -> {
                    SteamApp app = new SteamApp();
                    app.setAppId(e);
                    app.setName(newMap.get(e));
                    return app;
                }).collect(Collectors.toList());
                appRepository.saveAll(appsToAdd);
            }

            // Apps to remove
            existentApps.removeAll(newMap.keySet());
            if (!existentApps.isEmpty()) {
                appRepository.deleteApps(existentApps);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<SteamApp> find(String query) {
        return appRepository.search(query, null);
    }

    @Override
    @Transactional(readOnly = true)
    public SteamApp findOne(String query) {
        List<SteamApp> result = appRepository.search(query, PageRequest.of(0, 1));
        return result.isEmpty() ? null : result.get(0);
    }

    @Override
    @Transactional(readOnly = true)
    public SteamApp findByAppId(Long appId) {
        return appRepository.findByAppId(appId);
    }

    @Transactional
    @Override
    public SteamAppDetails getDetails(SteamApp steamApp, Locale locale) {
        if (steamApp == null) {
            return null;
        }
        Date date = DateTime.now().minusDays(1).toDate();
        String localeCode = locale.getDisplayLanguage(Locale.US).toLowerCase();
        SteamCache cache = cacheRepository.findByAppIdAndLocale(steamApp.getAppId(), localeCode);
        if (cache == null || cache.getUpdateDate().before(date)) {
            String url = String.format(DETAILS_ENDPOINT, steamApp.getAppId(), localeCode);
            try {
                ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
                if (HttpStatus.OK.equals(response.getStatusCode())) {
                    JsonNode rootNode = mapper.readTree(response.getBody());
                    rootNode = rootNode.get(String.valueOf(steamApp.getAppId()));
                    if (rootNode.get("success").asBoolean()) {
                        JsonNode data = rootNode.get("data");
                        SteamAppDetails details = mapper.treeToValue(data, SteamAppDetails.class);
                        if (cache == null) {
                            cache = new SteamCache();
                            cache.setAppId(steamApp.getAppId());
                            cache.setLocale(localeCode);
                            cache.setDetails(details);
                        }
                        cache.setUpdateDate(new Date());
                        cacheRepository.save(cache);
                        return details;
                    }
                }

            } catch (Exception e) {
                log.info("Exception parsing app details for {}", steamApp, e);
            }
        }
        return cache != null ? cache.getDetails() : null;
    }
}
