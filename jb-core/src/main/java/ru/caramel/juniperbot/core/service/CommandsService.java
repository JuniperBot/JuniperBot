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
package ru.caramel.juniperbot.core.service;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import ru.caramel.juniperbot.core.model.Command;
import ru.caramel.juniperbot.core.persistence.entity.CommandConfig;

import java.util.function.Function;

public interface CommandsService extends CommandSender {

    String EXECUTIONS_METER = "commands.executions.rate";

    String EXECUTIONS_COUNTER = "commands.executions.persist";

    void clear(Guild guild);

    void onMessageReceived(MessageReceivedEvent event);

    boolean sendMessage(MessageReceivedEvent event, CommandSender sender, Function<String, Boolean> commandCheck);

    void registerHandler(CommandHandler sender);

    boolean isApplicable(MessageReceivedEvent event, Command command, CommandConfig commandConfig);

    void resultEmotion(MessageReceivedEvent message, String emoji, String messageCode, Object... args);

    boolean isRestricted(MessageReceivedEvent event, CommandConfig commandConfig);

    boolean isRestricted(CommandConfig commandConfig, TextChannel channel);

    boolean isRestricted(CommandConfig commandConfig, Member member);

}
