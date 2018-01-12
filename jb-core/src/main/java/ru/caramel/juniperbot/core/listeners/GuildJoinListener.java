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
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.caramel.juniperbot.core.service.ConfigService;
import ru.caramel.juniperbot.core.service.MemberService;

@Component
public class GuildJoinListener extends DiscordEventListener {

    @Autowired
    private ConfigService configService;

    @Autowired
    private MemberService memberService;

    @Override
    public void onGuildJoin(GuildJoinEvent event) {
        configService.getOrCreate(event.getGuild());
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        memberService.getOrCreate(event.getMember());
    }
}