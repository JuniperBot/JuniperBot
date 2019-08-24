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
package ru.juniperbot.common.worker.command.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.joda.time.DateTimeZone;
import ru.juniperbot.common.persistence.entity.GuildConfig;
import ru.juniperbot.common.utils.LocaleUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Setter
public class BotContext {

    private GuildConfig config;

    @Getter(AccessLevel.PRIVATE)
    private final Map<String, Object> attributes = new ConcurrentHashMap<>();

    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T getAttribute(Class<T> type, String key) {
        Object value = getAttribute(key);
        return value != null && type.isAssignableFrom(value.getClass()) ? (T) value : null;
    }

    public Object putAttribute(String key, Object value) {
        return attributes.put(key, value);
    }

    public Object removeAttribute(String key) {
        return attributes.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T removeAttribute(Class<T> type, String key) {
        Object value = removeAttribute(key);
        return value != null && type.isAssignableFrom(value.getClass()) ? (T) value : null;
    }

    public DateTimeZone getTimeZone() {
        if (config != null) {
            try {
                return DateTimeZone.forID(config.getTimeZone());
            } catch (IllegalArgumentException e) {
                // fall down
            }
        }
        return DateTimeZone.UTC;
    }

    public String getCommandLocale() {
        return config != null ? config.getCommandLocale() : LocaleUtils.DEFAULT_LOCALE;
    }
}
