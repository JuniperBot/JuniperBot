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
import net.dv8tion.jda.core.entities.TextChannel;
import org.springframework.context.ApplicationContext;
import ru.caramel.juniperbot.core.messaging.placeholder.node.FunctionalNodePlaceholderResolver;
import ru.caramel.juniperbot.core.messaging.placeholder.node.SingletonNodePlaceholderResolver;
import ru.caramel.juniperbot.core.utils.TriFunction;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ChannelPlaceholderResolver extends FunctionalNodePlaceholderResolver<TextChannel> {

    private final static Map<String, TriFunction<TextChannel, Locale, ApplicationContext, ?>> ACCESSORS;

    static {
        Map<String, TriFunction<TextChannel, Locale, ApplicationContext, ?>> accessors = new HashMap<>();
        accessors.put("id", (t, l, c) -> t.getId());
        accessors.put("name", (t, l, c) -> t.getName());
        accessors.put("mention", (t, l, c) -> t.getAsMention());
        accessors.put("topic", (t, l, c) -> t.getTopic() != null ? t.getTopic() : "");
        accessors.put("position", (t, l, c) -> t.getPosition());
        accessors.put("createdAt", (t, l, c) -> DateTimePlaceholderResolver.of(t.getCreationTime(), l, t.getGuild(), c));
        ACCESSORS = Collections.unmodifiableMap(accessors);
    }

    private final TextChannel channel;

    public ChannelPlaceholderResolver(@NonNull TextChannel channel,
                                      @NonNull Locale locale,
                                      @NonNull ApplicationContext applicationContext) {
        super(ACCESSORS, locale, applicationContext);
        this.channel = channel;
    }

    @Override
    protected TextChannel getObject() {
        return channel;
    }

    @Override
    public Object getValue() {
        return channel.getName();
    }

    public static SingletonNodePlaceholderResolver of(@NonNull TextChannel channel,
                                                      @NonNull Locale locale,
                                                      @NonNull ApplicationContext context,
                                                      @NonNull String name) {
        return new SingletonNodePlaceholderResolver(name, new ChannelPlaceholderResolver(channel, locale, context));
    }
}
