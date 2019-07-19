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
package ru.caramel.juniperbot.core.message.resolver;

import lombok.NonNull;
import net.dv8tion.jda.core.entities.Guild;
import org.springframework.context.ApplicationContext;
import ru.caramel.juniperbot.core.message.resolver.node.FunctionalNodePlaceholderResolver;
import ru.caramel.juniperbot.core.message.resolver.node.SingletonNodePlaceholderResolver;
import ru.caramel.juniperbot.core.message.service.MessageService;
import ru.caramel.juniperbot.core.utils.TriFunction;

import java.util.Locale;
import java.util.Map;

public class GuildPlaceholderResolver extends FunctionalNodePlaceholderResolver<Guild> {

    private final static Map<String, TriFunction<Guild, Locale, ApplicationContext, ?>> ACCESSORS = Map.of(
            "id", (g, l, c) -> g.getId(),
            "name", (g, l, c) -> g.getName(),
            "iconUrl", (g, l, c) -> g.getIconUrl(),
            "region", (g, l, c) -> c.getBean(MessageService.class).getEnumTitle(g.getRegion()),
            "afkTimeout", (g, l, c) -> g.getAfkTimeout().getSeconds() / 60,
            "afkChannel", (g, l, c) -> g.getAfkChannel() != null ? g.getAfkChannel().getName() : "",
            "memberCount", (g, l, c) -> g.getMembers().size(),
            "createdAt", (g, l, c) -> DateTimePlaceholderResolver.of(g.getCreationTime(), l, g, c)
    );

    private final Guild guild;

    public GuildPlaceholderResolver(@NonNull Guild guild,
                                    @NonNull Locale locale,
                                    @NonNull ApplicationContext applicationContext) {
        super(ACCESSORS, locale, applicationContext);
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

    public static SingletonNodePlaceholderResolver of(@NonNull Guild guild,
                                                      @NonNull Locale locale,
                                                      @NonNull ApplicationContext context,
                                                      @NonNull String name) {
        return new SingletonNodePlaceholderResolver(name, new GuildPlaceholderResolver(guild, locale, context));
    }
}
