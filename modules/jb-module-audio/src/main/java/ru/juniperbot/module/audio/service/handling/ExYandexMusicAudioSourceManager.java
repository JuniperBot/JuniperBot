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
package ru.juniperbot.module.audio.service.handling;

import com.sedmelluq.discord.lavaplayer.source.yamusic.YandexMusicAudioSourceManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import ru.juniperbot.common.worker.configuration.WorkerProperties;

import javax.annotation.PostConstruct;

@Component
@Order(0)
public class ExYandexMusicAudioSourceManager extends YandexMusicAudioSourceManager {

    @Autowired
    private WorkerProperties workerProperties;

    @PostConstruct
    public void reload() {
        var configuration = workerProperties.getAudio().getYandexProxy();
        if (StringUtils.isNotEmpty(configuration.getHost())) {
            configureApiBuilder(builder -> {
                builder.setProxy(new HttpHost(configuration.getHost(), configuration.getPort()));
            });
        }
    }
}
