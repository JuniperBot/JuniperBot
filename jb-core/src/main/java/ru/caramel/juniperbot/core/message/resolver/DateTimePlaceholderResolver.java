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
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.entities.Guild;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.context.ApplicationContext;
import ru.caramel.juniperbot.core.common.persistence.GuildConfig;
import ru.caramel.juniperbot.core.common.service.ConfigService;
import ru.caramel.juniperbot.core.message.resolver.node.AbstractNodePlaceholderResolver;
import ru.caramel.juniperbot.core.message.resolver.node.SingletonNodePlaceholderResolver;

import java.time.OffsetDateTime;
import java.util.Locale;

@RequiredArgsConstructor
public class DateTimePlaceholderResolver extends AbstractNodePlaceholderResolver {

    @NonNull
    private final DateTime dateTime;

    @NonNull
    private final Locale locale;

    @NonNull
    private final Guild guild;

    @NonNull
    private final ApplicationContext applicationContext;

    private DateTimeZone timeZone;

    @Override
    public Object getValue() {
        return format(DateTimeFormat.mediumDateTime());
    }

    @Override
    public Object getChild(String name) {
        switch (name) {
            case "shortTime":
                return format(DateTimeFormat.shortTime());
            case "mediumTime":
                return format(DateTimeFormat.mediumTime());
            case "longTime":
                return format(DateTimeFormat.longTime());
            case "fullTime":
                return format(DateTimeFormat.fullTime());
            case "shortDate":
                return format(DateTimeFormat.shortDate());
            case "mediumDate":
                return format(DateTimeFormat.mediumDate());
            case "longDate":
                return format(DateTimeFormat.longDate()); // The same as medium
            case "fullDate":
                return format(DateTimeFormat.fullDate());
            case "shortDateTime":
                return format(DateTimeFormat.shortDateTime());
            case "mediumDateTime":
                return format(DateTimeFormat.mediumDateTime());
            case "longDateTime":
                return format(DateTimeFormat.longDateTime());
            case "fullDateTime":
                return format(DateTimeFormat.fullDateTime());
        }
        return null;
    }

    public String format(DateTimeFormatter baseFormatter) {
        return baseFormatter
                .withLocale(locale)
                .withZone(getTimeZone())
                .print(dateTime);
    }

    private DateTimeZone getTimeZone() {
        if (timeZone != null) {
            return timeZone;
        }
        if (guild != null) {
            GuildConfig config = applicationContext.getBean(ConfigService.class).get(guild);
            if (config != null) {
                try {
                    return timeZone = DateTimeZone.forID(config.getTimeZone());
                } catch (IllegalArgumentException e) {
                    // fall down
                }
            }
        }
        return timeZone = DateTimeZone.UTC;
    }

    public static DateTimePlaceholderResolver of(@NonNull OffsetDateTime offsetDateTime,
                                                 @NonNull Locale locale,
                                                 @NonNull Guild guild,
                                                 @NonNull ApplicationContext context) {
        return new DateTimePlaceholderResolver(new DateTime(offsetDateTime.toEpochSecond() * 1000), locale, guild, context);
    }

    public static SingletonNodePlaceholderResolver of(@NonNull DateTime dateTime,
                                                      @NonNull Locale locale,
                                                      @NonNull Guild guild,
                                                      @NonNull ApplicationContext context,
                                                      @NonNull String name) {
        return new SingletonNodePlaceholderResolver(name, new DateTimePlaceholderResolver(dateTime, locale, guild,
                context));
    }
}
