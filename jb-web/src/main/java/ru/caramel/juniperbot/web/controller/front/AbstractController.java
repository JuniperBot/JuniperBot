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
package ru.caramel.juniperbot.web.controller.front;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.VoiceChannel;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.ModelAndView;
import ru.caramel.juniperbot.core.model.exception.AccessDeniedException;
import ru.caramel.juniperbot.core.model.exception.NotFoundException;
import ru.caramel.juniperbot.core.persistence.entity.GuildConfig;
import ru.caramel.juniperbot.core.security.auth.DiscordTokenServices;
import ru.caramel.juniperbot.core.security.model.DiscordGuildDetails;
import ru.caramel.juniperbot.core.security.utils.SecurityUtils;
import ru.caramel.juniperbot.core.service.ConfigService;
import ru.caramel.juniperbot.core.service.DiscordService;
import ru.caramel.juniperbot.core.service.MessageService;
import ru.caramel.juniperbot.web.common.flash.Flash;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public abstract class AbstractController {

    @Autowired
    protected Flash flash;

    @Autowired
    protected DiscordService discordService;

    @Autowired
    protected DiscordTokenServices tokenServices;

    @Autowired
    protected ConfigService configService;

    @Autowired
    protected MessageService messageService;

    protected ModelAndView createModel(String modelName, long serverId) {
        return createModel(modelName, serverId, true);
    }

    protected ModelAndView createModel(String modelName, long serverId, boolean checkConnection) {
        ModelAndView mv = new ModelAndView(modelName);
        fillServerInfo(mv, serverId);
        if (checkConnection) {
            if (discordService.isConnected()) {
                boolean serverExists = discordService.getJda().getGuildById(serverId) != null;
                mv.addObject("serverAdded", serverExists);
                if (!serverExists) {
                    flash.warn("flash.warning.unknown-server.message");
                }
            } else {
                flash.warn("flash.warning.connection-problem.message");
            }
        }
        return mv;
    }

    private void fillServerInfo(ModelAndView mv, long serverId) {
        String serverName = null;
        String iconUrl = null;
        GuildConfig config = configService.getById(serverId);
        if (config != null) {
            serverName = config.getName();
            iconUrl = config.getIconUrl();
        }

        if (SecurityUtils.isAuthenticated() && (StringUtils.isEmpty(serverName) || StringUtils.isEmpty(iconUrl))) {
            DiscordGuildDetails details = tokenServices.getGuildById(serverId);
            if (StringUtils.isEmpty(serverName)) {
                serverName = details.getName();
            }
            if (StringUtils.isEmpty(iconUrl)) {
                iconUrl = details.getAvatarUrl();
            }
        }
        mv.addObject("serverId", serverId);
        mv.addObject("serverName", serverName);
        mv.addObject("serverIcon", iconUrl);
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

    protected boolean isGuildAuthorized(long id) {
        DiscordGuildDetails details = tokenServices.getGuildById(id);
        return details != null && tokenServices.hasPermission(details);
    }

    protected Guild getGuild(long id) {
        Guild guild = null;
        if (discordService.isConnected()) {
            guild = discordService.getJda().getGuildById(id);
        }
        return guild;
    }

    protected boolean hasPermission(long serverId, Permission permission) {
        Guild guild = getGuild(serverId);
        return guild != null && guild.getSelfMember().hasPermission(permission);
    }

    protected List<TextChannel> getTextChannels(long guildId) {
        Guild guild = getGuild(guildId);
        return guild != null ? guild.getTextChannels() : Collections.emptyList();
    }

    protected List<VoiceChannel> getVoiceChannels(long guildId) {
        Guild guild = getGuild(guildId);
        return guild != null ? guild.getVoiceChannels() : Collections.emptyList();
    }

    protected List<Role> getRoles(long guildId) {
        Guild guild = getGuild(guildId);
        return guild != null
                ? guild.getRoles().stream().filter(e -> !"@everyone".equals(e.getName())).collect(Collectors.toList())
                : Collections.emptyList();
    }
}
