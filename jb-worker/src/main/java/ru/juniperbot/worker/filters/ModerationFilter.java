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
package ru.juniperbot.worker.filters;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.juniperbot.worker.common.event.intercept.Filter;
import ru.juniperbot.worker.common.event.intercept.FilterChain;
import ru.juniperbot.worker.common.event.intercept.MemberMessageFilter;
import ru.juniperbot.worker.common.message.service.MessageService;
import ru.juniperbot.worker.common.modules.moderation.service.MuteService;

@Slf4j
@Order(Filter.PRE_FILTER)
@Component
public class ModerationFilter extends MemberMessageFilter {

    @Autowired
    private MessageService messageService;

    @Autowired
    private MuteService muteService;

    @Getter
    @Setter
    @Value("${feature.deleteMuted:true}")
    private boolean deleteMuted = true;

    @Override
    @Transactional
    public void doInternal(GuildMessageReceivedEvent event, FilterChain<GuildMessageReceivedEvent> chain) {
        if (deleteMuted && event.getGuild().getSelfMember().hasPermission(event.getChannel(), Permission.MESSAGE_MANAGE)
                && muteService.isMuted(event.getMember(), event.getChannel())) {
            messageService.delete(event.getMessage());
            return;
        }
        chain.doFilter(event);
    }
}
