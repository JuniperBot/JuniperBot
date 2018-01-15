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
import org.springframework.core.NamedInheritableThreadLocal;
import org.springframework.stereotype.Service;
import ru.caramel.juniperbot.core.service.ConfigService;
import ru.caramel.juniperbot.core.service.LocaleService;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class LocaleServiceImpl implements LocaleService {

    private final ThreadLocal<Locale> holder =
            new NamedInheritableThreadLocal<>("LocaleServiceImpl.Locale");

    @Getter
    private Map<String, Locale> supportedLocales;

    @Autowired
    private ConfigService configService;

    @PostConstruct
    public void init() {
        Map<String, Locale> localeMap = new HashMap<>();
        localeMap.put("en", Locale.US);
        localeMap.put("ru", Locale.forLanguageTag("ru"));
        supportedLocales = Collections.unmodifiableMap(localeMap);
    }

    @Override
    public Locale getLocale() {
        Locale locale = holder.get();
        return locale != null ? locale : getDefaultLocale();
    }

    @Override
    public Locale getDefaultLocale() {
        return supportedLocales.get(DEFAULT_LOCALE);
    }

    @Override
    public Locale getLocale(Guild guild) {
        return getLocale(guild.getIdLong());
    }

    @Override
    public Locale getLocale(long serverId) {
        return supportedLocales.getOrDefault(configService.getLocale(serverId), getDefaultLocale());
    }

    @Override
    public void setLocale(Locale locale) {
        if (locale == null) {
            holder.remove();
        } else {
            holder.set(locale);
        }
    }

    @Override
    public boolean isSupported(String tag) {
        return supportedLocales.containsKey(tag);
    }

    @Override
    public void initLocale(Guild guild) {
        if (guild != null) {
            setLocale(configService.getLocale(guild));
        }
    }

    @Override
    public void initLocale(long serverId) {
        setLocale(configService.getLocale(serverId));
    }

    private void setLocale(String tag) {
        setLocale(supportedLocales.get(tag));
    }
}
