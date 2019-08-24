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
package ru.juniperbot.api.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import ru.juniperbot.api.model.steam.GetAppListResponse;
import ru.juniperbot.api.model.steam.SteamAppEntry;
import ru.juniperbot.common.persistence.entity.SteamApp;
import ru.juniperbot.common.persistence.repository.SteamAppRepository;
import ru.juniperbot.common.utils.CommonUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class SteamDatabaseUpdater {

    @Autowired
    private SteamAppRepository appRepository;

    private RestTemplate restTemplate = new RestTemplate(CommonUtils.createRequestFactory());

    private static final String APPS_ENDPOINT = "https://api.steampowered.com/ISteamApps/GetAppList/v2/";

    @Transactional
    @Scheduled(cron = "0 0 0 * * ?")
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
}
