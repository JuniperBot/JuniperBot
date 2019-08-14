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
package ru.caramel.juniperbot.core.common.service;

import net.dv8tion.jda.api.entities.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.caramel.juniperbot.core.common.persistence.LocalUser;
import ru.caramel.juniperbot.core.common.persistence.LocalUserRepository;

import java.util.Objects;

@Service
public class UserServiceImpl implements UserService {

    private final Object $lock = new Object[0];

    @Autowired
    private LocalUserRepository repository;

    @Override
    @Transactional(readOnly = true)
    public LocalUser get(User user) {
        return repository.findByUserId(user.getId());
    }

    @Override
    @Transactional
    public LocalUser getOrCreate(User user) {
        if (!isApplicable(user)) {
            return null;
        }
        LocalUser localUser = get(user);
        if (localUser == null) {
            synchronized ($lock) {
                localUser = get(user);
                if (localUser == null) {
                    localUser = new LocalUser();
                    localUser.setUserId(user.getId());
                    updateIfRequired(user, localUser);
                    repository.flush();
                    return localUser;
                }
            }
        }
        return updateIfRequired(user, localUser);
    }

    @Override
    @Transactional
    public LocalUser updateIfRequired(User user, LocalUser localUser) {
        try {
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
        } catch (ObjectOptimisticLockingFailureException e) {
            // it's ok to ignore optlock here, anyway it will be updated later
        }
        return localUser;
    }

    @Override
    public boolean isApplicable(User user) {
        return user != null && !user.isBot();
    }
}
