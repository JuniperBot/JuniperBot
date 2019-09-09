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

import lombok.NonNull;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.juniperbot.common.persistence.entity.Gulag;
import ru.juniperbot.common.persistence.entity.LocalUser;
import ru.juniperbot.common.persistence.repository.GulagRepository;
import ru.juniperbot.common.service.GulagService;
import ru.juniperbot.common.service.UserService;

import java.util.Date;

@Service
public class GulagServiceImpl implements GulagService {

    @Autowired
    private GulagRepository repository;

    @Autowired
    private UserService userService;

    @Override
    @Transactional
    public boolean send(Member moderator, long showflake, String reason) {
        if (repository.existsBySnowflake(showflake)) {
            return false;
        }
        LocalUser user = userService.get(moderator.getUser());
        if (user == null) {
            return false;
        }
        Gulag gulag = new Gulag();
        gulag.setSnowflake(showflake);
        gulag.setModerator(user);
        gulag.setReason(reason);
        gulag.setDate(new Date());
        repository.save(gulag);
        return true;
    }

    @Override
    @Transactional
    public boolean send(Member moderator, Member member, String reason) {
        return send(moderator, member.getIdLong(), reason);
    }

    @Override
    @Transactional(readOnly = true)
    public Gulag getGulag(@NonNull Guild guild) {
        Gulag gulag = repository.findBySnowflake(guild.getOwnerIdLong());
        return gulag != null ? gulag : repository.findBySnowflake(guild.getIdLong());
    }
}
