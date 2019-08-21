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

import net.dv8tion.jda.api.entities.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.juniperbot.common.persistence.entity.LocalUser;
import ru.juniperbot.common.persistence.repository.LocalUserRepository;
import ru.juniperbot.common.service.UserService;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private LocalUserRepository repository;

    @Override
    @Transactional(readOnly = true)
    public LocalUser get(User user) {
        return repository.findByUserId(user.getId());
    }

    @Override
    @Transactional
    public LocalUser save(LocalUser user) {
        return repository.save(user);
    }

    @Override
    public boolean isApplicable(User user) {
        return user != null && !user.isBot();
    }
}
