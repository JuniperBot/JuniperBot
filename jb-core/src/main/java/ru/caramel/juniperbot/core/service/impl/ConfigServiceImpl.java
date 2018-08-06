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
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import ru.caramel.juniperbot.core.persistence.entity.GuildConfig;
import ru.caramel.juniperbot.core.persistence.repository.GuildConfigRepository;
import ru.caramel.juniperbot.core.service.ConfigService;
import ru.caramel.juniperbot.core.service.ContextService;
import ru.caramel.juniperbot.core.support.JbCacheManager;

import java.util.Objects;

@Service
public class ConfigServiceImpl extends AbstractDomainServiceImpl<GuildConfig, GuildConfigRepository> implements ConfigService {

    @Getter
    @Value("${commands.defaultPrefix:!}")
    private String defaultPrefix;

    @Autowired
    private JbCacheManager cacheManager;

    public ConfigServiceImpl(@Autowired GuildConfigRepository repository) {
        super(repository);
    }

    @Override
    @Transactional
    public GuildConfig getOrCreate(Guild guild) {
        GuildConfig config = super.getOrCreate(guild);
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
                repository.save(config);
            }
        } catch (ObjectOptimisticLockingFailureException e) {
            // it's ok to ignore optlock here, anyway it will be updated later
        }
        return config;
    }

    @Override
    @Transactional
    public GuildConfig getOrCreateCached(Guild guild) {
        GuildConfig config = cacheManager.get(GuildConfig.class, guild.getIdLong(), this::getOrCreate);
        if (!Objects.equals(config.getName(), guild.getName())
                || !Objects.equals(config.getIconUrl(), guild.getIconUrl())) {
            config = getOrCreate(guild);
            cacheManager.evict(GuildConfig.class, guild.getIdLong());
        }
        return config;
    }

    @Override
    @Transactional
    public String getPrefix(long guildId) {
        String prefix = repository.findPrefixByGuildId(guildId);
        return prefix != null ? prefix : getOrCreate(guildId).getPrefix();
    }

    @Override
    @Transactional(readOnly = true)
    public String getLocale(Guild guild) {
        return getLocale(guild.getIdLong());
    }

    @Transactional(readOnly = true)
    @Override
    public String getLocale(long guildId) {
        return repository.findLocaleByGuildId(guildId);
    }

    @Override
    protected GuildConfig createNew(long guildId) {
        GuildConfig config = new GuildConfig(guildId);
        config.setPrefix(defaultPrefix);
        config.setLocale(ContextService.DEFAULT_LOCALE);
        config.setTimeZone("Etc/Greenwich");
        return config;
    }
}
