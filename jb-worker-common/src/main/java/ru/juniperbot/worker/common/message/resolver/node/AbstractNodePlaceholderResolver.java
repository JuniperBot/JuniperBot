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
package ru.juniperbot.worker.common.message.resolver.node;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

public abstract class AbstractNodePlaceholderResolver implements NodePlaceholderResolver {

    @Override
    public Object resolveNode(String placeholderName) {
        if (StringUtils.isEmpty(placeholderName)) {
            return getValue();
        }

        String[] parts = placeholderName.split("\\.");
        String field = parts[0];

        Object value = getChild(field);
        if (value instanceof NodePlaceholderResolver) {
            String subPlaceholder = null;
            if (parts.length > 1) {
                parts = ArrayUtils.remove(parts, 0);
                subPlaceholder = StringUtils.join(parts, ".");
            }
            return ((NodePlaceholderResolver) value).resolveNode(subPlaceholder);
        }
        return value;
    }

    @Override
    public Object getValue() {
        return null;
    }
}
