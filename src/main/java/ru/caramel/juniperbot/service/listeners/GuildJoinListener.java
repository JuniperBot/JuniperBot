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
package ru.caramel.juniperbot.service.listeners;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.caramel.juniperbot.integration.discord.DiscordEventListener;
import ru.caramel.juniperbot.persistence.entity.GuildConfig;
import ru.caramel.juniperbot.ranking.service.RankingService;
import ru.caramel.juniperbot.service.ConfigService;
import ru.caramel.juniperbot.service.MessageService;
import ru.caramel.juniperbot.service.PermissionsService;

@Component
public class GuildJoinListener extends DiscordEventListener {

    @Autowired
    private ConfigService configService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private PermissionsService permissionsService;

    @Autowired
    private RankingService rankingService;

    @Override
    public void onGuildJoin(GuildJoinEvent event) {
        Guild guild = event.getGuild();
        boolean exists = configService.exists(guild.getIdLong());
        GuildConfig config = configService.getOrCreate(guild); // initialize
        rankingService.sync(guild);
        for (TextChannel channel : guild.getTextChannels()) {
            if (permissionsService.checkPermission(channel, Permission.MESSAGE_WRITE)) {
                messageService.onMessage(channel, exists ? "discord.welcome.again" : "discord.welcome");
                break;
            }
        }
    }
}