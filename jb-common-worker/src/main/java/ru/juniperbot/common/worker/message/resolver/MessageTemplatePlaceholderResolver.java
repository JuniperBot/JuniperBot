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
package ru.juniperbot.common.worker.message.resolver;

import org.springframework.util.PropertyPlaceholderHelper;

import java.util.*;

public class MessageTemplatePlaceholderResolver implements PropertyPlaceholderHelper.PlaceholderResolver {

    private final Map<String, String> values = new HashMap<>();

    private final Set<? extends PropertyPlaceholderHelper.PlaceholderResolver> resolvers;

    public MessageTemplatePlaceholderResolver(PropertyPlaceholderHelper.PlaceholderResolver... resolvers) {
        this(resolvers != null ? Arrays.asList(resolvers) : Collections.emptySet());
    }

    public MessageTemplatePlaceholderResolver(Collection<? extends PropertyPlaceholderHelper.PlaceholderResolver> resolvers) {
        this.resolvers = Set.copyOf(resolvers);
    }

    @Override
    public String resolvePlaceholder(String placeholderName) {
        return values.computeIfAbsent(placeholderName, p -> {
            for (PropertyPlaceholderHelper.PlaceholderResolver resolver : resolvers) {
                String value = resolver.resolvePlaceholder(p);
                if (value != null) {
                    return value;
                }
            }
            return null;
        });
    }
}
