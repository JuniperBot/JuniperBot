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

import lombok.NonNull;
import net.dv8tion.jda.core.entities.Guild;
import ru.caramel.juniperbot.core.messaging.placeholder.node.FunctionalNodePlaceholderResolver;
import ru.caramel.juniperbot.core.messaging.placeholder.node.SingletonNodePlaceholderResolver;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class GuildPlaceholderResolver extends FunctionalNodePlaceholderResolver<Guild> {

    private final static Map<String, Function<Guild, ?>> ACCESSORS;

    static {
        Map<String, Function<Guild, ?>> accessors = new HashMap<>();
        accessors.put("id", Guild::getId);
        accessors.put("name", Guild::getName);
        ACCESSORS = Collections.unmodifiableMap(accessors);
    }

    private final Guild guild;

    public GuildPlaceholderResolver(Guild guild) {
        super(ACCESSORS);
        this.guild = guild;
    }

    @Override
    protected Guild getObject() {
        return guild;
    }

    @Override
    public Object getValue() {
        return guild.getName();
    }

    public static SingletonNodePlaceholderResolver of(@NonNull Guild guild, @NonNull String name) {
        return new SingletonNodePlaceholderResolver(name, new GuildPlaceholderResolver(guild));
    }
}
