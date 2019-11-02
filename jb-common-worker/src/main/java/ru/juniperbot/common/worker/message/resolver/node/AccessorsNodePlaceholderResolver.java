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
package ru.juniperbot.common.worker.message.resolver.node;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;

import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

@RequiredArgsConstructor
public abstract class AccessorsNodePlaceholderResolver<T> extends AbstractNodePlaceholderResolver {

    @NonNull
    protected final Locale locale;

    @NonNull
    protected final ApplicationContext context;

    private Map<String, Supplier<?>> accessors;

    protected abstract T getObject();

    @Override
    public Object getChild(String name) {
        Supplier<?> function = getAccessors().get(name);
        return function != null ? function.get() : null;
    }

    private Map<String, Supplier<?>> getAccessors() {
        if (accessors != null) {
            return accessors;
        }
        synchronized (this) {
            if (accessors == null) {
                accessors = createAccessors();
            }
        }
        return accessors;
    }

    protected abstract Map<String, Supplier<?>> createAccessors();
}
