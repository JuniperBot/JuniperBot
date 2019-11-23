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
package ru.juniperbot.module.misc.commands

import com.udojava.evalex.AbstractFunction
import com.udojava.evalex.Expression
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import ru.juniperbot.common.worker.command.model.AbstractCommand
import ru.juniperbot.common.worker.command.model.BotContext
import ru.juniperbot.common.worker.command.model.DiscordCommand
import java.math.BigDecimal

@DiscordCommand(
        key = "discord.command.math.key",
        description = "discord.command.math.desc",
        group = ["discord.command.group.utility"],
        priority = 30)
class MathCommand : AbstractCommand() {

    override fun doCommand(message: GuildMessageReceivedEvent, context: BotContext, query: String): Boolean {
        if (query.isEmpty()) {
            messageService.onTempEmbedMessage(message.channel, 5, "discord.command.math.help")
            return false
        }

        try {
            val expression = createExpression(query)
            val result = expression.eval()

            val resultMessage = messageService.getMessage("discord.command.math.result", query, result)
            if (resultMessage.length > MessageEmbed.TEXT_MAX_LENGTH) {
                messageService.onError(message.channel, null, "discord.command.math.length")
                return true
            }
            val builder = messageService.baseEmbed.setDescription(resultMessage)
            messageService.sendMessageSilent({ message.channel.sendMessage(it) }, builder.build())
        } catch (e: Expression.ExpressionException) {
            messageService.onError(message.channel, null, "discord.command.math.error", query, e.message)
            return false
        } catch (e: ArithmeticException) {
            messageService.onError(message.channel, null, "discord.command.math.error", query, e.message)
            return false
        }

        return true
    }

    private fun createExpression(query: String): Expression {
        val expression = Expression(query).setPrecision(32)
        // limit fact calculation
        expression.addFunction(object : AbstractFunction("FACT", 1, false) {
            override fun eval(parameters: List<BigDecimal?>): BigDecimal {
                val value = parameters[0] ?: throw ArithmeticException("Operand may not be null")
                val number = value.toInt()
                if (number > 100) {
                    throw ArithmeticException("Cannot calculate factorial more than 100")
                }
                var factorial = BigDecimal.ONE
                for (i in 1..number) {
                    factorial = factorial.multiply(BigDecimal(i))
                }
                return factorial
            }
        })
        return expression
    }
}
