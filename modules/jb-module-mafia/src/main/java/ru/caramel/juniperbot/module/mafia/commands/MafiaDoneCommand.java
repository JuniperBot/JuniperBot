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

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import ru.caramel.juniperbot.core.model.BotContext;
import ru.caramel.juniperbot.core.model.DiscordCommand;

@DiscordCommand(key = "discord.command.mafia.done.key",
        description = "discord.command.mafia.done.desc",
        group = "mafia.name",
        priority = 5)
public class MafiaDoneCommand extends MafiaCommandAsync {

    @Override
    public void doCommandAsync(MessageReceivedEvent message, BotContext context, String query) {
        if (mafiaService.done(message.getAuthor(), message.getTextChannel())) {
            ok(message);
        } else  {
            fail(message);
        }
    }
}
