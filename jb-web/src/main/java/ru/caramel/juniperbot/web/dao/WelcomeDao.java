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
import ru.caramel.juniperbot.web.dto.config.WelcomeDto;
import ru.juniperbot.common.persistence.entity.WelcomeMessage;
import ru.juniperbot.common.service.WelcomeService;

@Service
public class WelcomeDao extends AbstractDao {

    @Autowired
    private WelcomeService welcomeService;

    @Autowired
    private MessageTemplateDao templateDao;

    @Transactional
    public WelcomeDto get(long guildId) {
        WelcomeMessage welcomeMessage = welcomeService.getByGuildId(guildId);
        return welcomeMessage != null ? apiMapper.getWelcomeDto(welcomeMessage) : new WelcomeDto();
    }

    @Transactional
    public void save(WelcomeDto dto, long guildId) {
        WelcomeMessage welcomeMessage = welcomeService.getOrCreate(guildId);

        welcomeMessage.setJoinTemplate(templateDao.updateOrCreate(dto.getJoinTemplate(),
                welcomeMessage.getJoinTemplate()));

        welcomeMessage.setJoinDmTemplate(templateDao.updateOrCreate(dto.getJoinDmTemplate(),
                welcomeMessage.getJoinDmTemplate()));

        welcomeMessage.setLeaveTemplate(templateDao.updateOrCreate(dto.getLeaveTemplate(),
                welcomeMessage.getLeaveTemplate()));

        apiMapper.updateWelcome(dto, welcomeMessage);
        welcomeService.save(welcomeMessage);
    }
}
