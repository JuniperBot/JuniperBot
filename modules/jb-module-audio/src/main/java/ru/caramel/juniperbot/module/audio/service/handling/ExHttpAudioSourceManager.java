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
package ru.caramel.juniperbot.module.audio.service.handling;

import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.juniperbot.common.persistence.entity.HttpWhitelist;
import ru.juniperbot.common.persistence.repository.HttpWhitelistRepository;

import javax.annotation.PostConstruct;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

@Component
public class ExHttpAudioSourceManager extends HttpAudioSourceManager {

    @Autowired
    private HttpWhitelistRepository repository;

    private String[] allowedHighDomains;

    private Set<String> allowedHosts;

    @PostConstruct
    public void reload() {
        allowedHosts = repository.findAllDomains();
        allowedHighDomains = makeSubDomains(allowedHosts);
    }

    @Override
    public AudioItem loadItem(DefaultAudioPlayerManager manager, AudioReference reference) {
        try {
            URI uri = new URI(reference.identifier);
            if (allowedHosts.contains(uri.getHost()) || StringUtils.endsWithAny(uri.getHost(), allowedHighDomains)) {
                return super.loadItem(manager, reference);
            }
        } catch (URISyntaxException e) {
            // fall down
        }
        return null;
    }

    public void add(String domain) {
        if (!repository.existsByDomain(domain)) {
            HttpWhitelist whitelist = new HttpWhitelist(domain);
            repository.save(whitelist);
            allowedHosts.add(domain);
            allowedHighDomains = makeSubDomains(allowedHosts);
        }
    }

    private String[] makeSubDomains(Set<String> allowedHosts) {
        if (CollectionUtils.isEmpty(allowedHosts)) {
            return null;
        }
        String[] highDomains = new String[allowedHosts.size()];
        int i = 0;
        for (String host : allowedHosts) {
            highDomains[i++] = "." + host;
        }
        return highDomains;
    }
}
