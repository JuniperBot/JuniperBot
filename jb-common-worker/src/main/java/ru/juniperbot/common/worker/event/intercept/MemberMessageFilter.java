/*
 * This file is part of JuniperBot.
 *
 * JuniperBot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * JuniperBot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with JuniperBot. If not, see <http://www.gnu.org/licenses/>.
 */
package ru.juniperbot.common.worker.event.intercept;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.MessageType;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

@Slf4j
public abstract class MemberMessageFilter implements Filter<GuildMessageReceivedEvent> {

    @Override
    public final void doFilter(GuildMessageReceivedEvent event, FilterChain<GuildMessageReceivedEvent> chain) {
        try {
            if (!event.isWebhookMessage()
                    && !event.getAuthor().isBot()
                    && event.getMember() != null
                    && event.getChannel() != null
                    && event.getMessage().getType() == MessageType.DEFAULT) {
                doInternal(event, chain);
                return;
            }
        } catch (Throwable e) {
            log.warn("Unexpected filter error", e);
        }
        chain.doFilter(event);
    }

    protected abstract void doInternal(GuildMessageReceivedEvent event, FilterChain<GuildMessageReceivedEvent> chain);
}
