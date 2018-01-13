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
package ru.caramel.juniperbot.core.service.impl;

import net.dv8tion.jda.core.entities.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.caramel.juniperbot.core.persistence.entity.LocalUser;
import ru.caramel.juniperbot.core.persistence.repository.LocalUserRepository;
import ru.caramel.juniperbot.core.service.UserService;

import java.util.Objects;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private LocalUserRepository repository;

    @Override
    public LocalUser getOrCreate(User user) {
        if (!isApplicable(user)) {
            return null;
        }
        LocalUser localUser = repository.findByUserId(user.getId());
        if (localUser == null) {
            localUser = new LocalUser();
            localUser.setUserId(user.getId());
        }
        return updateIfRequired(user, localUser);
    }

    @Override
    public LocalUser updateIfRequired(User user, LocalUser localUser) {
        boolean shouldSave = false;
        if (localUser.getId() == null) {
            shouldSave = true;
        }

        if (user != null) {

            if (!Objects.equals(user.getName(), localUser.getName())) {
                localUser.setName(user.getName());
                shouldSave = true;
            }

            if (!Objects.equals(user.getDiscriminator(), localUser.getDiscriminator())) {
                localUser.setDiscriminator(user.getDiscriminator());
                shouldSave = true;
            }

            if (!Objects.equals(user.getAvatarUrl(), localUser.getAvatarUrl())) {
                localUser.setAvatarUrl(user.getAvatarUrl());
                shouldSave = true;
            }
        }

        if (shouldSave) {
            repository.save(localUser);
        }
        return localUser;
    }

    @Override
    public boolean isApplicable(User user) {
        return user != null && !user.isBot();
    }
}
