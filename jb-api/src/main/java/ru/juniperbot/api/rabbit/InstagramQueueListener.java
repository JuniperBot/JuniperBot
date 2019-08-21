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
package ru.juniperbot.api.rabbit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.juniperbot.api.subscriptions.integrations.InstagramService;
import ru.juniperbot.common.configuration.RabbitConfiguration;
import ru.juniperbot.common.model.InstagramProfile;

@EnableRabbit
@Component
@Slf4j
public class InstagramQueueListener {

    @Autowired
    private InstagramService instagramService;

    @RabbitListener(queues = RabbitConfiguration.QUEUE_INSTAGRAM_PROFILE_REQUEST)
    public InstagramProfile getStatus(String dummy) {
        InstagramProfile profile = null;
        try {
            profile = instagramService.getRecent();
        } catch (Exception e) {
            // fall down
        }
        return profile != null ? profile : new InstagramProfile();
    }
}
