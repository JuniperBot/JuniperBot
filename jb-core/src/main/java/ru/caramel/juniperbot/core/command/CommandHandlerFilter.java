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
package ru.caramel.juniperbot.core.command;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.entities.MessageType;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.caramel.juniperbot.core.command.model.CommandHandler;
import ru.caramel.juniperbot.core.event.intercept.Filter;
import ru.caramel.juniperbot.core.event.intercept.FilterChain;

import java.util.List;

@Slf4j
@Component
public class CommandHandlerFilter implements Filter<GuildMessageReceivedEvent> {

    @Autowired
    private List<CommandHandler> handlers;

    @Override
    @Transactional
    public void doFilter(GuildMessageReceivedEvent event, FilterChain<GuildMessageReceivedEvent> chain) {
        if (!event.getAuthor().isBot() && event.getMessage().getType() == MessageType.DEFAULT) {
            for (CommandHandler handler : handlers) {
                try {
                    if (handler.handleMessage(event)) {
                        break;
                    }
                } catch (Throwable e) {
                    log.warn("Could not handle command", e);
                }
            }
        }
        chain.doFilter(event);
    }
}
