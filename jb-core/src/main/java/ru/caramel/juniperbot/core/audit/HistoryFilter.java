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
package ru.caramel.juniperbot.core.audit;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import ru.caramel.juniperbot.core.audit.service.HistoryService;
import ru.caramel.juniperbot.core.event.intercept.Filter;
import ru.caramel.juniperbot.core.event.intercept.FilterChain;

@Slf4j
@Order(Filter.POST_FILTER)
@Component
public class HistoryFilter implements Filter<GuildMessageReceivedEvent> {

    @Autowired
    private HistoryService historyService;

    @Override
    public void doFilter(GuildMessageReceivedEvent event, FilterChain<GuildMessageReceivedEvent> chain) {
        if (!event.getAuthor().isBot() && !event.isWebhookMessage()) {
            historyService.onMessageCreate(event.getMessage());
        }
        chain.doFilter(event);
    }
}
