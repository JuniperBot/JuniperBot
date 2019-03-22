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
package ru.caramel.juniperbot.core.command.model;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import ru.caramel.juniperbot.core.common.model.exception.DiscordException;

public interface Command {

    boolean doCommand(MessageReceivedEvent message, BotContext context, String content) throws DiscordException;

    boolean isAvailable(User user, Member member, Guild guild);

    default String getKey() {
        if (!getClass().isAnnotationPresent(DiscordCommand.class)) {
            return null;
        }
        return getClass().getAnnotation(DiscordCommand.class).key();
    }

    default Permission[] getPermissions() {
        if (!getClass().isAnnotationPresent(DiscordCommand.class)) {
            return null;
        }
        return getClass().getAnnotation(DiscordCommand.class).permissions();
    }
}
