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

import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import ru.juniperbot.common.utils.ArrayUtil;
import ru.juniperbot.common.worker.command.model.AbstractCommand;
import ru.juniperbot.common.worker.command.model.BotContext;
import ru.juniperbot.common.worker.command.model.DiscordCommand;

@DiscordCommand(key = "discord.command.coin.key",
        description = "discord.command.coin.desc",
        group = "discord.command.group.fun",
        priority = 18)
public class CoinCommand extends AbstractCommand {

    @Override
    public boolean doCommand(GuildMessageReceivedEvent message, BotContext context, String query) {
        String headsKeys[] = messageService.getMessage("discord.command.coin.heads", context.getCommandLocale())
                .split(",");
        String tailsKeys[] = messageService.getMessage("discord.command.coin.tails", context.getCommandLocale())
                .split(",");

        if (!ArrayUtil.containsIgnoreCase(headsKeys, query) && !ArrayUtil.containsIgnoreCase(tailsKeys, query)) {
            messageService.onMessage(message.getChannel(), "discord.command.coin.help", headsKeys[0], tailsKeys[0]);
            return false;
        }

        boolean headsBet = ArrayUtil.containsIgnoreCase(headsKeys, query);
        boolean headsOutcome = Math.random() < 0.5;

        String outCome = headsOutcome ? (headsBet ? query : headsKeys[0]) : tailsKeys[0];
        String resultMessage = headsBet == headsOutcome ? "discord.command.coin.result.win" : "discord.command.coin.result.lose";
        messageService.onMessage(message.getChannel(), resultMessage, query, outCome);
        return true;
    }
}
