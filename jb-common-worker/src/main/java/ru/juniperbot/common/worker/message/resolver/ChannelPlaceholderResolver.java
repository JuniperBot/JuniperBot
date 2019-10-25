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
package ru.juniperbot.common.worker.message.resolver;

import lombok.NonNull;
import net.dv8tion.jda.api.entities.TextChannel;
import org.springframework.context.ApplicationContext;
import ru.juniperbot.common.worker.message.resolver.node.AccessorsNodePlaceholderResolver;
import ru.juniperbot.common.worker.message.resolver.node.SingletonNodePlaceholderResolver;

import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

public class ChannelPlaceholderResolver extends AccessorsNodePlaceholderResolver<TextChannel> {

    private final TextChannel channel;

    public ChannelPlaceholderResolver(@NonNull TextChannel channel,
                                      @NonNull Locale locale,
                                      @NonNull ApplicationContext applicationContext) {
        super(locale, applicationContext);
        this.channel = channel;
    }

    @Override
    protected Map<String, Supplier<?>> createAccessors() {
        return Map.of(
                "id", channel::getId,
                "name", channel::getName,
                "mention", channel::getAsMention,
                "topic", () -> channel.getTopic() != null ? channel.getTopic() : "",
                "position", () -> channel.getPosition() + 1,
                "createdAt", () -> DateTimePlaceholderResolver.of(channel.getTimeCreated(), locale, channel.getGuild(), context)
        );
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
