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
package ru.caramel.juniperbot.core.messaging.placeholder.node;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.springframework.context.ApplicationContext;
import ru.caramel.juniperbot.core.utils.TriFunction;

import java.util.Locale;
import java.util.Map;

@AllArgsConstructor
public abstract class FunctionalNodePlaceholderResolver<T> extends AbstractNodePlaceholderResolver {

    @NonNull
    private final Map<String, TriFunction<T, Locale, ApplicationContext, ?>> accessors;

    @NonNull
    protected final Locale locale;

    @NonNull
    protected final ApplicationContext applicationContext;

    protected abstract T getObject();

    @Override
    public Object getChild(String name) {
        TriFunction<T, Locale, ApplicationContext, ?> function = accessors.get(name);
        return function != null ? function.apply(getObject(), locale, applicationContext) : null;
    }
}
