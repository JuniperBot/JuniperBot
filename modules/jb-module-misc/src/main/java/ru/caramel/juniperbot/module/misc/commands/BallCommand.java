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
package ru.caramel.juniperbot.module.misc.commands;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import ru.caramel.juniperbot.core.model.AbstractCommand;
import ru.caramel.juniperbot.core.model.BotContext;
import ru.caramel.juniperbot.core.model.DiscordCommand;

@DiscordCommand(key = "discord.command.ball.key",
        description = "discord.command.ball.desc",
        group = "discord.command.group.utility",
        priority = 18)
public class BallCommand extends AbstractCommand {

    @Override
    public boolean doCommand(MessageReceivedEvent message, BotContext context, String query) {
        if (StringUtils.isEmpty(query)) {
            messageService.onMessage(message.getChannel(), "discord.command.ball.question");
            return false;
        }
        int number = RandomUtils.nextInt(0, 20);
        messageService.onMessage(message.getChannel(), String.format("discord.command.ball.n%s", number));
        return true;
    }
}
