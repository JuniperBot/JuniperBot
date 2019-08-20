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

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import ru.juniperbot.common.model.exception.DiscordException;

public interface Command {

    boolean doCommand(GuildMessageReceivedEvent message, BotContext context, String content) throws DiscordException;

    boolean isAvailable(User user, Member member, Guild guild);

    DiscordCommand getAnnotation();

    default String getKey() {
        DiscordCommand annotation = getAnnotation();
        return annotation != null ? annotation.key() : null;
    }

    default boolean isHidden() {
        DiscordCommand annotation = getAnnotation();
        return annotation != null && annotation.hidden();
    }

    default Permission[] getPermissions() {
        DiscordCommand annotation = getAnnotation();
        return annotation != null ? annotation.permissions() : null;
    }
}
