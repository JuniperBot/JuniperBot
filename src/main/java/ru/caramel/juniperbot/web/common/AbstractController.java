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
package ru.caramel.juniperbot.web.common;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.VoiceChannel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.ModelAndView;
import ru.caramel.juniperbot.integration.discord.DiscordClient;
import ru.caramel.juniperbot.model.exception.AccessDeniedException;
import ru.caramel.juniperbot.model.exception.NotFoundException;
import ru.caramel.juniperbot.security.auth.DiscordTokenServices;
import ru.caramel.juniperbot.security.model.DiscordGuildDetails;
import ru.caramel.juniperbot.service.ConfigService;
import ru.caramel.juniperbot.service.MessageService;
import ru.caramel.juniperbot.web.common.flash.Flash;

import java.util.Collections;
import java.util.List;

public abstract class AbstractController {

    @Autowired
    protected Flash flash;

    @Autowired
    protected DiscordClient discordClient;

    @Autowired
    protected DiscordTokenServices tokenServices;

    @Autowired
    protected ConfigService configService;

    @Autowired
    protected MessageService messageService;

    protected ModelAndView createModel(String modelName, long serverId) {
        ModelAndView mv = new ModelAndView(modelName);
        mv.addObject("serverId", serverId);
        DiscordGuildDetails details = tokenServices.getGuildById(serverId);
        if (details != null) {
            mv.addObject("serverName", details.getName());
        }
        if (discordClient.isConnected()) {
            boolean serverExists = discordClient.getJda().getGuildById(serverId) != null;
            mv.addObject("serverAdded", serverExists);
            if (!serverExists) {
                flash.warn("flash.warning.unknown-server.message");
            }
        } else {
            flash.warn("flash.warning.connection-problem.message");
        }
        return mv;
    }

    protected void validateGuildId(long id) {
        DiscordGuildDetails details = tokenServices.getGuildById(id);
        if (details == null) {
            throw new NotFoundException();
        }
        if (!tokenServices.hasPermission(details)) {
            throw new AccessDeniedException();
        }
    }

    protected Guild getGuild(long id) {
        Guild guild = null;
        if (discordClient.isConnected()) {
            guild = discordClient.getJda().getGuildById(id);
        }
        return guild;
    }

    protected List<TextChannel> getTextChannels(long guildId) {
        Guild guild = getGuild(guildId);
        return guild != null ? guild.getTextChannels() : Collections.emptyList();
    }

    protected List<VoiceChannel> getVoiceChannels(long guildId) {
        Guild guild = getGuild(guildId);
        return guild != null ? guild.getVoiceChannels() : Collections.emptyList();
    }
}
