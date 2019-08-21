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
package ru.juniperbot.common.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.juniperbot.common.configuration.CommonProperties;
import ru.juniperbot.common.persistence.entity.GuildConfig;
import ru.juniperbot.common.persistence.repository.GuildConfigRepository;
import ru.juniperbot.common.service.ConfigService;
import ru.juniperbot.common.utils.LocaleUtils;

@Service
public class ConfigServiceImpl extends AbstractDomainServiceImpl<GuildConfig, GuildConfigRepository> implements ConfigService {

    @Autowired
    private CommonProperties commonProperties;

    public ConfigServiceImpl(@Autowired GuildConfigRepository repository) {
        super(repository, true);
    }

    @Override
    @Transactional(readOnly = true)
    public String getPrefix(long guildId) {
        String prefix = repository.findPrefixByGuildId(guildId);
        return prefix != null ? prefix : commonProperties.getDiscord().getDefaultPrefix();
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
        config.setPrefix(commonProperties.getDiscord().getDefaultPrefix());
        config.setColor(commonProperties.getDiscord().getDefaultAccentColor());
        config.setLocale(LocaleUtils.DEFAULT_LOCALE);
        config.setCommandLocale(LocaleUtils.DEFAULT_LOCALE);
        config.setTimeZone("Etc/Greenwich");
        return config;
    }

    @Override
    protected Class<GuildConfig> getDomainClass() {
        return GuildConfig.class;
    }
}
