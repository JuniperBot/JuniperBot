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
package ru.juniperbot.module.mafia.commands;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import ru.juniperbot.common.worker.command.model.BotContext;
import ru.juniperbot.common.worker.command.model.DiscordCommand;

@DiscordCommand(key = "discord.command.mafia.start.key",
        description = "discord.command.mafia.start.desc",
        group = "discord.command.group.mafia",
        permissions = {
                Permission.MESSAGE_WRITE,
                Permission.MESSAGE_EMBED_LINKS,
                Permission.MESSAGE_ADD_REACTION,
                Permission.MANAGE_CHANNEL
        },
        priority = 1)
public class MafiaStartCommand extends MafiaCommandAsync {

    @Override
    protected void doCommandAsync(GuildMessageReceivedEvent message, BotContext context, String query) {
        if (!mafiaService.start(message.getAuthor(), message.getChannel())) {
            messageService.onError(message.getChannel(), "mafia.alreadyStarted");
            fail(message);
        }
    }
}
