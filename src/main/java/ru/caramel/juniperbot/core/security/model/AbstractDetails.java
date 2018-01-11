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
package ru.caramel.juniperbot.core.security.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Map;
import java.util.function.Consumer;

public abstract class AbstractDetails implements Serializable {

    @Getter
    @Setter(AccessLevel.PROTECTED)
    protected String id;

    @SuppressWarnings("unchecked")
    protected static  <T> void setValue(Class<T> type, Map<Object, Object> map, String name, Consumer<T> setter) {
        Object value = map.get(name);
        if (value == null) {
            return;
        }
        if (!type.isAssignableFrom(value.getClass())) {
            throw new IllegalStateException(String.format("Wrong user details class type for %s. Found [%s], expected [%s]",
                    name, value.getClass().getName(), type.getName()));
        }
        setter.accept((T) value);
    }
}
