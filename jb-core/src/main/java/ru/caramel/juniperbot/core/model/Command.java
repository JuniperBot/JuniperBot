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
package ru.caramel.juniperbot.core.model;

import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.apache.commons.lang3.ArrayUtils;
import ru.caramel.juniperbot.core.model.enums.CommandGroup;
import ru.caramel.juniperbot.core.model.enums.CommandSource;
import ru.caramel.juniperbot.core.model.exception.DiscordException;
import ru.caramel.juniperbot.core.persistence.entity.GuildConfig;

public interface Command {

    boolean doCommand(MessageReceivedEvent message, BotContext context, String content) throws DiscordException;

    default boolean isApplicable(MessageChannel channel, GuildConfig config) {
        if (!getClass().isAnnotationPresent(DiscordCommand.class)) {
            return false;
        }
        DiscordCommand command = getClass().getAnnotation(DiscordCommand.class);
        if (config != null) {
            if (ArrayUtils.contains(config.getDisabledCommands(), command.key())) {
                return false;
            }
            if (CommandGroup.RANKING.equals(command.group()) && !config.getRankingConfig().isEnabled()) {
                return false;
            }
        }
        if (command.source().length == 0) {
            return true;
        }
        CommandSource source = channel instanceof TextChannel ? CommandSource.GUILD : CommandSource.DM;
        return ArrayUtils.contains(command.source(), source);
    }
}
