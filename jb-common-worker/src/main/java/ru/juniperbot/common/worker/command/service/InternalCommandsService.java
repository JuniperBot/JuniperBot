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
package ru.juniperbot.common.worker.command.service;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import ru.juniperbot.common.persistence.entity.CommandConfig;
import ru.juniperbot.common.worker.command.model.Command;

public interface InternalCommandsService extends CommandsService, CommandHandler {

    String EXECUTIONS_METER = "commands.executions.rate";

    String EXECUTIONS_COUNTER = "commands.executions.persist";

    boolean isApplicable(Command command, CommandConfig commandConfig, User user, Member member, TextChannel channel);

    boolean isRestricted(String rawKey, TextChannel channel, Member member);

}
