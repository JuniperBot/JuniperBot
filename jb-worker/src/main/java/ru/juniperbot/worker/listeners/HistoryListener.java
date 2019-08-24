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
package ru.juniperbot.worker.listeners;

import net.dv8tion.jda.api.events.message.guild.GuildMessageDeleteEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageUpdateEvent;
import org.springframework.beans.factory.annotation.Autowired;
import ru.juniperbot.common.worker.event.DiscordEvent;
import ru.juniperbot.common.worker.event.listeners.DiscordEventListener;
import ru.juniperbot.common.worker.modules.audit.service.ActionsHolderService;
import ru.juniperbot.common.worker.modules.audit.service.HistoryService;

@DiscordEvent
public class HistoryListener extends DiscordEventListener {

    @Autowired
    private HistoryService historyService;

    @Autowired
    private ActionsHolderService actionsHolderService;

    @Override
    public void onGuildMessageUpdate(GuildMessageUpdateEvent event) {
        if (!event.getAuthor().isBot()) {
            historyService.onMessageUpdate(event.getMessage());
        }
    }

    @Override
    public void onGuildMessageDelete(GuildMessageDeleteEvent event) {
        if (!actionsHolderService.isOwnDeleted(event.getChannel().getId(), event.getMessageId())) {
            historyService.onMessageDelete(event.getChannel(), event.getMessageId());
        }
    }
}