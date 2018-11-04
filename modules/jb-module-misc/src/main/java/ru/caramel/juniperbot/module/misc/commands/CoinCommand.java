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
import ru.caramel.juniperbot.core.model.AbstractCommand;
import ru.caramel.juniperbot.core.model.BotContext;
import ru.caramel.juniperbot.core.model.DiscordCommand;

@DiscordCommand(key = "discord.command.coin.key",
        description = "discord.command.coin.desc",
        group = "discord.command.group.fun",
        priority = 18)
public class CoinCommand extends AbstractCommand {

    @Override
    public boolean doCommand(MessageReceivedEvent message, BotContext context, String query) {
        String headsKey = messageService.getMessage("discord.command.coin.heads");
        String tailsKey = messageService.getMessage("discord.command.coin.tails");
        if (!headsKey.equalsIgnoreCase(query) && !tailsKey.equalsIgnoreCase(query)) {
            messageService.onMessage(message.getChannel(), "discord.command.coin.help", headsKey, tailsKey);
            return false;
        }

        boolean headsBet = headsKey.equalsIgnoreCase(query);
        boolean headsOutcome = Math.random() < 0.5;

        String outCome = headsOutcome ? headsKey : tailsKey;
        String resultMessage = headsBet == headsOutcome ? "discord.command.coin.result.win" : "discord.command.coin.result.lose";
        messageService.onMessage(message.getChannel(), resultMessage, query, outCome);
        return true;
    }
}
