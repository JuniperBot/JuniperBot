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
package ru.caramel.juniperbot.module.moderation.commands;

import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import ru.caramel.juniperbot.core.model.BotContext;
import ru.caramel.juniperbot.core.model.DiscordCommand;
import ru.caramel.juniperbot.module.ranking.service.RankingService;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@DiscordCommand(key = "discord.command.mod.import.key",
        description = "discord.command.mod.import.desc",
        group = "discord.command.group.moderation",
        source = ChannelType.TEXT,
        hidden = true)
public class ImportCommand extends ModeratorCommandAsync {

    private static final Logger log = LoggerFactory.getLogger(ImportCommand.class);

    @Autowired
    private RankingService rankingService;

    private final Set<Guild> active = Collections.synchronizedSet(new HashSet<>());

    @Override
    protected void doCommandAsync(MessageReceivedEvent event, BotContext context, String query) {
        try {
            synchronized (active) {
                if (active.contains(event.getGuild())) {
                    messageService.onMessage(event.getChannel(), "discord.command.mod.import.inProgress");
                    return;
                }
                active.add(event.getGuild());
            }
            messageService.onMessage(event.getChannel(), "discord.command.mod.import.started");
            long count = rankingService.importRanking(event.getGuild());
            messageService.onMessage(event.getChannel(), "discord.command.mod.import.end",
                    event.getMember().getAsMention(), count);
        } catch (Exception e) {
            log.error("Could not import", e);
            messageService.onMessage(event.getChannel(), "discord.command.mod.import.failed");
        } finally {
            active.remove(event.getGuild());
        }
    }
}
