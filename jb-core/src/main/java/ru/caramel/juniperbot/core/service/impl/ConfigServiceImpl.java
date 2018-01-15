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

import lombok.Getter;
import net.dv8tion.jda.core.entities.Guild;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import ru.caramel.juniperbot.core.persistence.entity.GuildConfig;
import ru.caramel.juniperbot.core.persistence.repository.GuildConfigRepository;
import ru.caramel.juniperbot.core.service.ConfigService;
import ru.caramel.juniperbot.core.service.LocaleService;

import java.util.Objects;

@Service
public class ConfigServiceImpl implements ConfigService {

    @Getter
    @Value("${commands.defaultPrefix:!}")
    private String defaultPrefix;

    @Autowired
    private GuildConfigRepository repository;

    @Override
    @Transactional
    public void save(GuildConfig config) {
        repository.save(config);
    }

    @Override
    @Transactional
    public GuildConfig getOrCreate(long serverId) {
        return createIfMissing(getById(serverId), serverId);
    }

    @Override
    @Transactional
    public GuildConfig getOrCreate(Guild guild) {
        Assert.notNull(guild, "Guild cannot be null");
        GuildConfig config = getOrCreate(guild.getIdLong());

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
            repository.save(config);
        }
        return config;
    }

    @Override
    @Transactional
    public GuildConfig getById(long serverId) {
        return repository.findByGuildId(serverId);
    }

    @Override
    public String getPrefix(long serverId) {
        String prefix = repository.findPrefixByGuildId(serverId);
        return prefix != null ? prefix : getOrCreate(serverId).getPrefix();
    }

    @Transactional(readOnly = true)
    @Override
    public boolean exists(long serverId) {
        return repository.existsByGuildId(serverId);
    }

    @Override
    public String getLocale(Guild guild) {
        return getLocale(guild.getIdLong());
    }

    @Transactional(readOnly = true)
    @Override
    public String getLocale(long serverId) {
        return repository.findLocaleByGuildId(serverId);
    }

    private GuildConfig createIfMissing(GuildConfig config, long serverId) {
        if (config == null) {
            config = new GuildConfig(serverId);
            config.setPrefix(defaultPrefix);
            config.setLocale(LocaleService.DEFAULT_LOCALE);
            repository.save(config);
        }
        return config;
    }
}
