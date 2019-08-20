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
package ru.caramel.juniperbot.module.ranking;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import ru.caramel.juniperbot.module.ranking.service.RankingService;
import ru.juniperbot.worker.common.event.intercept.Filter;
import ru.juniperbot.worker.common.event.intercept.FilterChain;
import ru.juniperbot.worker.common.event.intercept.MemberMessageFilter;
import ru.juniperbot.common.service.RankingConfigService;

@Slf4j
@Order(Filter.POST_FILTER)
@Component
public class RankingFilter extends MemberMessageFilter {

    @Autowired
    private RankingService rankingService;

    @Override
    public void doInternal(GuildMessageReceivedEvent event, FilterChain<GuildMessageReceivedEvent> chain) {
        rankingService.onMessage(event);
        chain.doFilter(event);
    }
}
