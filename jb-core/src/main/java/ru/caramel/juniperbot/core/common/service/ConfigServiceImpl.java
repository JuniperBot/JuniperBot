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

import lombok.Getter;
import net.dv8tion.jda.core.entities.Guild;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.caramel.juniperbot.core.common.persistence.GuildConfig;
import ru.caramel.juniperbot.core.common.persistence.GuildConfigRepository;
import ru.caramel.juniperbot.core.event.service.ContextService;

import java.util.Objects;

@Service
public class ConfigServiceImpl extends AbstractDomainServiceImpl<GuildConfig, GuildConfigRepository> implements ConfigService {

    @Getter
    @Value("${discord.defaultPrefix:!}")
    private String defaultPrefix;

    @Value("${discord.accentColor:#FFA550}")
    private String accentColor;

    public ConfigServiceImpl(@Autowired GuildConfigRepository repository) {
        super(repository, true);
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
    @Transactional(readOnly = true)
    public String getPrefix(long guildId) {
        String prefix = repository.findPrefixByGuildId(guildId);
        return prefix != null ? prefix : defaultPrefix;
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
    @Transactional(readOnly = true)
    public String getColor(long guildId) {
        return repository.findColorByGuildId(guildId);
    }

    @Override
    protected GuildConfig createNew(long guildId) {
        GuildConfig config = new GuildConfig(guildId);
        config.setPrefix(defaultPrefix);
        config.setColor(accentColor);
        config.setLocale(ContextService.DEFAULT_LOCALE);
        config.setCommandLocale(ContextService.DEFAULT_LOCALE);
        config.setTimeZone("Etc/Greenwich");
        return config;
    }

    @Override
    protected Class<GuildConfig> getDomainClass() {
        return GuildConfig.class;
    }
}
