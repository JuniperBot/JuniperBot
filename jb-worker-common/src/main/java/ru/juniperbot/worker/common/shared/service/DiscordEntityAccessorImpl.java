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
package ru.juniperbot.worker.common.shared.service;

import net.dv8tion.jda.api.entities.Guild;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.juniperbot.common.persistence.entity.GuildConfig;
import ru.juniperbot.common.persistence.repository.GuildConfigRepository;
import ru.juniperbot.common.service.ConfigService;

import java.util.Objects;

@Service
public class DiscordEntityAccessorImpl implements DiscordEntityAccessor {

    @Autowired
    private ConfigService configService;

    @Override
    @Transactional
    public GuildConfig get(Guild guild) {
        return configService.getByGuildId(guild.getIdLong());
    }

    @Override
    @Transactional
    public GuildConfig getOrCreate(Guild guild) {
        GuildConfig config = configService.getOrCreate(guild.getIdLong());
        try {
            boolean shouldSave = false;
            if (!Objects.equals(config.getName(), guild.getName())) {
                config.setName(guild.getName());
                shouldSave = true;
            }
            if (!Objects.equals(config.getIconUrl(), guild.getIconUrl())) {
                config.setIconUrl(guild.getIconUrl());
                shouldSave = true;
            }
            if (shouldSave) {
                configService.save(config);
            }
        } catch (ObjectOptimisticLockingFailureException e) {
            // it's ok to ignore optlock here, anyway it will be updated later
        }
        return config;
    }
}
