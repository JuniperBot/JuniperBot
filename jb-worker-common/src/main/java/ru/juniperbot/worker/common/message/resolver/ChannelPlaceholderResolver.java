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
package ru.juniperbot.worker.common.message.resolver;

import lombok.NonNull;
import net.dv8tion.jda.api.entities.TextChannel;
import org.springframework.context.ApplicationContext;
import ru.juniperbot.worker.common.message.resolver.node.FunctionalNodePlaceholderResolver;
import ru.juniperbot.worker.common.message.resolver.node.SingletonNodePlaceholderResolver;
import ru.juniperbot.worker.common.shared.support.TriFunction;

import java.util.Locale;
import java.util.Map;

public class ChannelPlaceholderResolver extends FunctionalNodePlaceholderResolver<TextChannel> {

    private final static Map<String, TriFunction<TextChannel, Locale, ApplicationContext, ?>> ACCESSORS = Map.of(
            "id", (t, l, c) -> t.getId(),
            "name", (t, l, c) -> t.getName(),
            "mention", (t, l, c) -> t.getAsMention(),
            "topic", (t, l, c) -> t.getTopic() != null ? t.getTopic() : "",
            "position", (t, l, c) -> t.getPosition() + 1,
            "createdAt", (t, l, c) -> DateTimePlaceholderResolver.of(t.getTimeCreated(), l, t.getGuild(), c)
    );

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
        return channel.getAsMention();
    }

    public static SingletonNodePlaceholderResolver of(@NonNull TextChannel channel,
                                                      @NonNull Locale locale,
                                                      @NonNull ApplicationContext context,
                                                      @NonNull String name) {
        return new SingletonNodePlaceholderResolver(name, new ChannelPlaceholderResolver(channel, locale, context));
    }
}
