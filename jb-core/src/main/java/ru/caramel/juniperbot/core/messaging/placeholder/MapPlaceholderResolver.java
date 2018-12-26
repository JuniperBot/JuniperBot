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
package ru.caramel.juniperbot.core.messaging.placeholder;

import org.springframework.util.PropertyPlaceholderHelper;

import java.util.HashMap;
import java.util.Map;

public class MapPlaceholderResolver implements PropertyPlaceholderHelper.PlaceholderResolver {

    private Map<String, String> values = new HashMap<>();

    public String put(String key, String value) {
        return values.put(key, value);
    }

    @Override
    public String resolvePlaceholder(String placeholderName) {
        return values.get(placeholderName);
    }
}
