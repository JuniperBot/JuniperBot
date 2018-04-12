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
package ru.caramel.juniperbot.module.junipost.service;

import me.postaddict.instagram.scraper.Instagram;
import me.postaddict.instagram.scraper.cookie.CookieHashSet;
import me.postaddict.instagram.scraper.cookie.DefaultCookieJar;
import me.postaddict.instagram.scraper.interceptor.ErrorInterceptor;
import me.postaddict.instagram.scraper.model.Account;
import me.postaddict.instagram.scraper.model.Media;
import me.postaddict.instagram.scraper.model.PageInfo;
import me.postaddict.instagram.scraper.model.PageObject;
import okhttp3.OkHttpClient;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.List;

@Service
public class InstagramService {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstagramService.class);

    @Value("${instagram.pollUserId}")
    private Long pollUserId;

    @Value("${instagram.ttl:30000}")
    private Long ttl;

    @Value("${instagram.updateInterval:30000}")
    private Long updateInterval;

    @Autowired
    private TaskScheduler scheduler;

    @Autowired
    private PostService postService;

    private Instagram instagram;

    private List<Media> cache;

    private long latestUpdate;

    private Account account;

    @PostConstruct
    public void init() {
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .addInterceptor(new ErrorInterceptor())
                .cookieJar(new DefaultCookieJar(new CookieHashSet()))
                .build();
        instagram = new Instagram(httpClient);
        scheduler.scheduleWithFixedDelay(this::update, updateInterval);
    }

    public List<Media> getRecent() {
        try {
            long currentTimestamp = System.currentTimeMillis();
            if (currentTimestamp > latestUpdate + ttl) {
                synchronized (this) {
                    account = instagram.getAccountById(pollUserId);
                    PageObject<Media> medias = instagram.getMedias(pollUserId, 1, new PageInfo(true, ""));
                    if (medias != null && medias.getCount() != null && medias.getCount() > 0) {
                        cache = medias.getNodes();
                        latestUpdate = currentTimestamp;
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error("Could not get Instagram data", e);
        }
        return cache;
    }

    public Account getAccount() {
        synchronized (this) {
            try {
                if (account == null) {
                    account = instagram.getAccountById(pollUserId);
                }
            } catch (IOException e) {
                LOGGER.error("Could not get Instagram account data", e);
            }
        }
        return account;
    }

    private void update() {
        List<Media> medias = getRecent();
        if (CollectionUtils.isNotEmpty(medias)) {
            postService.onInstagramUpdated(medias);
        }
    }
}
