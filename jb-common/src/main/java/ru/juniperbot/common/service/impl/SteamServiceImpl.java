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
package ru.juniperbot.common.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import ru.juniperbot.common.model.steam.SteamAppDetails;
import ru.juniperbot.common.persistence.entity.SteamApp;
import ru.juniperbot.common.persistence.entity.SteamCache;
import ru.juniperbot.common.persistence.repository.SteamAppRepository;
import ru.juniperbot.common.persistence.repository.SteamCacheRepository;
import ru.juniperbot.common.service.SteamService;
import ru.juniperbot.common.utils.CommonUtils;

import java.util.Date;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
public class SteamServiceImpl implements SteamService {

    private static final ObjectMapper mapper = new ObjectMapper();

    private static final String DETAILS_ENDPOINT = "https://store.steampowered.com/api/appdetails?appids=%s&l=%s";

    @Autowired
    private SteamAppRepository appRepository;

    @Autowired
    private SteamCacheRepository cacheRepository;

    private RestTemplate restTemplate = new RestTemplate(CommonUtils.createRequestFactory());

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
