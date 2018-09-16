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
package ru.caramel.juniperbot.core.listeners;

import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
import org.springframework.beans.factory.annotation.Autowired;
import ru.caramel.juniperbot.core.model.DiscordEvent;
import ru.caramel.juniperbot.core.persistence.entity.GuildConfig;
import ru.caramel.juniperbot.core.service.ConfigService;
import ru.caramel.juniperbot.core.service.ContextService;

@DiscordEvent(priority = 0)
public class GuildListener extends DiscordEventListener {

    @Autowired
    private ConfigService configService;

    @Autowired
    private ContextService contextService;

    @Override
    public void onGuildJoin(GuildJoinEvent event) {
        GuildConfig config = configService.getOrCreate(event.getGuild());
        switch (event.getGuild().getRegion()) {
            case RUSSIA:
                config.setLocale(ContextService.RU_LOCALE);
                config.setCommandLocale(ContextService.RU_LOCALE);
                break;
            default:
                config.setLocale(ContextService.DEFAULT_LOCALE);
                config.setCommandLocale(ContextService.DEFAULT_LOCALE);
                break;
        }
        configService.save(config);
        contextService.initContext(event.getGuild()); // reinit context with updated locale
    }

}