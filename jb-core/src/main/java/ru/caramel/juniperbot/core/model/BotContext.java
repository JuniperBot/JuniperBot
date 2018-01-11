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
package ru.caramel.juniperbot.core.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.core.entities.Guild;
import ru.caramel.juniperbot.core.persistence.entity.GuildConfig;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Setter
public class BotContext {

    private Guild guild;

    private String prefix;

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
}
