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
package ru.caramel.juniperbot.core.modules.ranking.service;

import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberLeaveEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.caramel.juniperbot.core.modules.ranking.persistence.entity.RankingConfig;
import ru.caramel.juniperbot.core.service.listeners.DiscordEventListener;

@Component
public class RankingListener extends DiscordEventListener {

    @Autowired
    private RankingService rankingService;

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        rankingService.onMessage(event);
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        rankingService.getOrCreateMember(event.getMember());
    }

    @Override
    public void onGuildMemberLeave(GuildMemberLeaveEvent event) {
        RankingConfig config = rankingService.getConfig(event.getGuild());
        if (config != null && config.isResetOnLeave()) {
            rankingService.setLevel(event.getGuild().getIdLong(), event.getMember().getUser().getIdLong(), 0);
        }
    }
}
