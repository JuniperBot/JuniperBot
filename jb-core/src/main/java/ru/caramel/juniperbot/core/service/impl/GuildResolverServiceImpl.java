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
package ru.caramel.juniperbot.core.service.impl;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.events.Event;
import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;
import ru.caramel.juniperbot.core.service.GuildResolverService;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

@Service
public class GuildResolverServiceImpl implements GuildResolverService {

    private Map<Class<? extends Event>, Method> guildAccessors = new HashMap<>();

    @Override
    public Guild getGuild(Event event) {
        if (event == null) {
            return null;
        }
        Class<? extends Event> clazz = event.getClass();
        Method method = guildAccessors.computeIfAbsent(clazz, e -> ReflectionUtils.findMethod(clazz, "getGuild"));
        if (method != null) {
            Object result = ReflectionUtils.invokeMethod(method, event);
            if (result instanceof Guild) {
                return (Guild) result;
            }
        }
        return null;
    }
}
