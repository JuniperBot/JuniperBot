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
package ru.juniperbot.module.misc.commands;

import com.udojava.evalex.AbstractFunction;
import com.udojava.evalex.Expression;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.apache.commons.lang3.StringUtils;
import ru.juniperbot.common.worker.command.model.AbstractCommand;
import ru.juniperbot.common.worker.command.model.BotContext;
import ru.juniperbot.common.worker.command.model.DiscordCommand;

import java.math.BigDecimal;
import java.util.List;

@DiscordCommand(key = "discord.command.math.key",
        description = "discord.command.math.desc",
        group = "discord.command.group.utility",
        priority = 30)
public class MathCommand extends AbstractCommand {

    @Override
    public boolean doCommand(GuildMessageReceivedEvent message, BotContext context, String query) {
        if (StringUtils.isEmpty(query)) {
            messageService.onTempEmbedMessage(message.getChannel(), 5, "discord.command.math.help");
            return false;
        }

        try {
            Expression expression = createExpression(query);
            BigDecimal result = expression.eval();

            String resultMessage = messageService.getMessage("discord.command.math.result", query, result);
            if (resultMessage.length() > MessageEmbed.TEXT_MAX_LENGTH) {
                messageService.onError(message.getChannel(), null, "discord.command.math.length");
                return true;
            }
            EmbedBuilder builder = messageService.getBaseEmbed();
            builder.setDescription(resultMessage);
            messageService.sendMessageSilent(message.getChannel()::sendMessage, builder.build());
        } catch (Expression.ExpressionException | ArithmeticException e) {
            messageService.onError(message.getChannel(), null,"discord.command.math.error", query, e.getMessage());
            return false;
        }
        return true;
    }

    private Expression createExpression(String query) {
        Expression expression = new Expression(query).setPrecision(32);
        // limit fact calculation
        expression.addFunction(new AbstractFunction("FACT", 1, false) {
            @Override
            public BigDecimal eval(List<BigDecimal> parameters) {
                BigDecimal value = parameters.get(0);
                if (value == null) {
                    throw new ArithmeticException("Operand may not be null");
                }
                int number = value.intValue();
                if (number > 100) {
                    throw new ArithmeticException("Cannot calculate factorial more than 100");
                }
                BigDecimal factorial = BigDecimal.ONE;
                for (int i = 1; i <= number; i++) {
                    factorial = factorial.multiply(new BigDecimal(i));
                }
                return factorial;
            }
        });
        return expression;
    }
}
