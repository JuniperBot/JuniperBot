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
package ru.juniperbot.common.worker.message.resolver.node;

import org.springframework.lang.Nullable;
import org.springframework.util.PropertyPlaceholderHelper;

public interface NodePlaceholderResolver extends PropertyPlaceholderHelper.PlaceholderResolver {

    Object getValue();

    Object getChild(String name);

    Object resolveNode(String placeholderName);

    @Override
    default String resolvePlaceholder(@Nullable String placeholderName) {
        Object value = resolveNode(placeholderName);
        return value != null ? String.valueOf(value) : null;
    }
}
