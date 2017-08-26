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
