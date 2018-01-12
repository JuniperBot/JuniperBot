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
package ru.caramel.juniperbot.web.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.caramel.juniperbot.module.welcome.persistence.entity.WelcomeMessage;
import ru.caramel.juniperbot.module.welcome.persistence.repository.WelcomeMessageRepository;
import ru.caramel.juniperbot.web.dto.WelcomeMessageDto;

@Service
public class WelcomeDao extends AbstractDao {

    @Autowired
    private WelcomeMessageRepository welcomeMessageRepository;

    @Transactional(readOnly = true)
    public WelcomeMessageDto getWelcomeMessageDto(long serverId) {
        WelcomeMessage welcomeMessage = welcomeMessageRepository.findByGuildId(serverId);
        return welcomeMessage != null ? mapper.getMessageDto(welcomeMessage) : new WelcomeMessageDto();
    }

    @Transactional(readOnly = true)
    public WelcomeMessage getWelcomeMessage(long serverId) {
        return welcomeMessageRepository.findByGuildId(serverId);
    }

    @Transactional
    public void saveWelcomeMessage(WelcomeMessageDto dto, long serverId) {
        WelcomeMessage welcomeMessage = welcomeMessageRepository.findByGuildId(serverId);
        if (welcomeMessage == null) {
            welcomeMessage = new WelcomeMessage();
            welcomeMessage.setGuildConfig(configService.getOrCreate(serverId));
        }
        mapper.updateWelcomeMessage(dto, welcomeMessage);
        welcomeMessageRepository.save(welcomeMessage);
    }
}
