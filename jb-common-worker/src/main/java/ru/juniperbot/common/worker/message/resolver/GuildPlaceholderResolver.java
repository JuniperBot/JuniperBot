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
import net.dv8tion.jda.api.entities.Guild;
import org.springframework.context.ApplicationContext;
import ru.juniperbot.common.worker.message.resolver.node.AccessorsNodePlaceholderResolver;
import ru.juniperbot.common.worker.message.resolver.node.SingletonNodePlaceholderResolver;
import ru.juniperbot.common.worker.message.service.MessageService;

import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

public class GuildPlaceholderResolver extends AccessorsNodePlaceholderResolver<Guild> {

    private final Guild guild;

    public GuildPlaceholderResolver(@NonNull Guild guild,
                                    @NonNull Locale locale,
                                    @NonNull ApplicationContext applicationContext) {
        super(locale, applicationContext);
        this.guild = guild;
    }

    @Override
    protected Map<String, Supplier<?>> createAccessors() {
        return Map.of(
                "id", guild::getId,
                "name", guild::getName,
                "iconUrl", guild::getIconUrl,
                "region", () -> context.getBean(MessageService.class).getEnumTitle(guild.getRegion()),
                "afkTimeout", () -> guild.getAfkTimeout().getSeconds() / 60,
                "afkChannel", () -> guild.getAfkChannel() != null ? guild.getAfkChannel().getName() : "",
                "memberCount", () -> guild.getMembers().size(),
                "createdAt", () -> DateTimePlaceholderResolver.of(guild.getTimeCreated(), locale, guild, context),
                "owner", () -> MemberPlaceholderResolver.of(guild.getOwner(), locale, context)
        );
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
