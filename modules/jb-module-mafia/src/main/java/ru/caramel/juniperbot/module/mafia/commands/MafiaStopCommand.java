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
package ru.caramel.juniperbot.module.mafia.commands;

import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import ru.juniperbot.common.worker.command.model.BotContext;
import ru.juniperbot.common.worker.command.model.DiscordCommand;

@DiscordCommand(key = "discord.command.mafia.stop.key",
        description = "discord.command.mafia.stop.desc",
        group = "discord.command.group.mafia",
        priority = 10)
public class MafiaStopCommand extends MafiaCommand {

    @Override
    public boolean doCommand(GuildMessageReceivedEvent message, BotContext context, String query) {
        return mafiaService.stop(message.getMember(), message.getChannel()) || fail(message);
    }
}
