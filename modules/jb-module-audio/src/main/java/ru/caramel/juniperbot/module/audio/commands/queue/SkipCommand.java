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
package ru.caramel.juniperbot.module.audio.commands.queue;

import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import ru.caramel.juniperbot.core.command.model.BotContext;
import ru.caramel.juniperbot.core.command.model.DiscordCommand;
import ru.caramel.juniperbot.core.common.model.exception.DiscordException;
import ru.caramel.juniperbot.module.audio.commands.AudioCommand;

@DiscordCommand(
        key = SkipCommand.KEY,
        description = "discord.command.skip.desc",
        group = "discord.command.group.music",
        priority = 110)
public class SkipCommand extends AudioCommand {

    public static final String KEY = "discord.command.skip.key";

    @Override
    public boolean doInternal(GuildMessageReceivedEvent message, BotContext context, String content) throws DiscordException {
        playerService.skipTrack(message.getMember(), message.getGuild());
        return true;
    }
}
