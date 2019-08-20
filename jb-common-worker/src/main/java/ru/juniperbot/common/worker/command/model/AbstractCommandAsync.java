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
package ru.juniperbot.common.worker.command.model;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import ru.juniperbot.common.model.exception.DiscordException;
import ru.juniperbot.common.model.exception.ValidationException;

@Slf4j
public abstract class AbstractCommandAsync extends AbstractCommand {

    @Override
    public final boolean doCommand(GuildMessageReceivedEvent message, BotContext context, String content) {
        contextService.withContextAsync(message.getGuild(), () -> {
            try {
                doCommandAsync(message, context, content);
            } catch (ValidationException e) {
                messageService.onEmbedMessage(message.getChannel(), e.getMessage(), e.getArgs());
            } catch (DiscordException e) {
                messageService.onError(message.getChannel(),
                        messageService.hasMessage(e.getMessage()) ? e.getMessage() : "discord.global.error");
                log.error("Command {} execution error", this, e);
            }
        });
        return true;
    }

    protected abstract void doCommandAsync(GuildMessageReceivedEvent message, BotContext context, String content)
            throws DiscordException;
}
