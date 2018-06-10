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
package ru.caramel.juniperbot.web.controller.api.base;

import net.dv8tion.jda.core.entities.Guild;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.caramel.juniperbot.core.model.exception.AccessDeniedException;
import ru.caramel.juniperbot.core.model.exception.NotFoundException;
import ru.caramel.juniperbot.core.service.ConfigService;
import ru.caramel.juniperbot.core.service.DiscordService;
import ru.caramel.juniperbot.web.security.auth.DiscordTokenServices;
import ru.caramel.juniperbot.web.security.model.DiscordGuildDetails;
import ru.caramel.juniperbot.web.service.MapperService;

@RequestMapping("api")
public abstract class BaseRestController {

    @Autowired
    protected DiscordService discordService;

    @Autowired
    protected DiscordTokenServices tokenServices;

    @Autowired
    protected ConfigService configService;

    @Autowired
    protected MapperService mapperService;

    protected void validateGuildId(long id) {
        DiscordGuildDetails details = tokenServices.getGuildById(id);
        if (details == null) {
            throw new NotFoundException();
        }
        if (!tokenServices.hasPermission(details)) {
            throw new AccessDeniedException();
        }
    }

    protected boolean isGuildAuthorized(long id) {
        DiscordGuildDetails details = tokenServices.getGuildById(id);
        return details != null && tokenServices.hasPermission(details);
    }

    protected Guild getGuild(long id) {
        Guild guild = null;
        if (discordService.isConnected(id)) {
            guild = discordService.getShardManager().getGuildById(id);
        }
        return guild;
    }
}
