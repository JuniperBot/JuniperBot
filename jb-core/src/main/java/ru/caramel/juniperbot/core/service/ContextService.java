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
package ru.caramel.juniperbot.core.service;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.requests.RestAction;

import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

public interface ContextService {

    String DEFAULT_LOCALE = "en";

    String RU_LOCALE = "ru";

    void setLocale(Locale locale);

    Locale getLocale();

    Locale getDefaultLocale();

    Locale getLocale(Guild guild);

    Locale getLocale(long serverId);

    Map<String, Locale> getSupportedLocales();

    boolean isSupported(String tag);

    void initContext(Event event);

    void initContext(Guild guild);

    void withContext(long serverId, Runnable action);

    void withContext(Guild guild, Runnable action);

    void initContext(User user);

    void initContext(long serverId);

    void resetContext();

    void execute(long serverId, Runnable action);

    void execute(Guild guild, Runnable action);

    <T> void queue(Guild guild, RestAction<T> action, Consumer<T> success);

}
