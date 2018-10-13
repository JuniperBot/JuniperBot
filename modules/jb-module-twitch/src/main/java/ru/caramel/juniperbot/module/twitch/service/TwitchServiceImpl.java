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
package ru.caramel.juniperbot.module.twitch.service;

import me.philippheuer.twitch4j.TwitchClient;
import me.philippheuer.twitch4j.TwitchClientBuilder;
import me.philippheuer.twitch4j.model.User;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.caramel.juniperbot.core.persistence.entity.WebHook;
import ru.caramel.juniperbot.core.service.WebHookService;
import ru.caramel.juniperbot.module.twitch.persistence.entity.TwitchConnection;
import ru.caramel.juniperbot.module.twitch.persistence.repository.TwitchConnectionRepository;

import javax.annotation.PostConstruct;

@Service
public class TwitchServiceImpl implements TwitchService {

    private static final Logger log = LoggerFactory.getLogger(TwitchServiceImpl.class);

    @Value("${integrations.twitch.clientId:}")
    private String clientId;

    @Value("${integrations.twitch.secret:}")
    private String secret;

    @Value("${integrations.twitch.oauthKey:}")
    private String oauthKey;

    @Value("${integrations.twitch.updateInterval:}")
    private Long updateInterval;

    @Autowired
    private WebHookService webHookService;

    @Autowired
    private TaskScheduler scheduler;

    @Autowired
    private TwitchConnectionRepository repository;

    private TwitchClient client;

    @PostConstruct
    public void init() {
        if (StringUtils.isEmpty(clientId) || StringUtils.isEmpty(secret)) {
            log.warn("No valid Twitch credentials provided");
            return;
        }
        try {
            client = TwitchClientBuilder.init()
                    .withClientId(clientId)
                    .withClientSecret(secret)
                    .withCredential(oauthKey)
                    .connect();
            scheduler.scheduleWithFixedDelay(this::update, updateInterval);
        } catch (Exception e) {
            log.error("Twitch connection error", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public TwitchConnection find(long id) {
        return repository.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public TwitchConnection save(TwitchConnection connection) {
        return repository.save(connection);
    }

    @Override
    public void delete(TwitchConnection connection) {
        webHookService.delete(connection.getGuildId(), connection.getWebHook());
        repository.delete(connection);
    }

    @Override
    @Transactional
    public TwitchConnection create(long guildId, User user) {
        TwitchConnection connection = new TwitchConnection();
        connection.setGuildId(guildId);
        connection.setUserId(user.getId());
        connection.setLogin(user.getName());
        connection.setName(user.getDisplayName());
        connection.setDescription(user.getBio());
        connection.setIconUrl(user.getLogo());

        WebHook hook = new WebHook();
        hook.setEnabled(true);
        connection.setWebHook(hook);
        return repository.save(connection);
    }

    private void update() {
        log.debug("Starting grabbing Twitch channels...");
    }

    @Override
    public User getUser(String userName) {
        return client != null ? client.getUserEndpoint().getUserByUserName(userName) : null;
    }
}
