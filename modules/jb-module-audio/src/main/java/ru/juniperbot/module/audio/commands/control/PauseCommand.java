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
package ru.juniperbot.module.audio.commands.control;

import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import ru.juniperbot.common.worker.command.model.BotContext;
import ru.juniperbot.common.worker.command.model.DiscordCommand;
import ru.juniperbot.module.audio.commands.AudioCommand;

@DiscordCommand(
        key = PauseCommand.KEY,
        description = "discord.command.pause.desc",
        group = "discord.command.group.music",
        priority = 105)
public class PauseCommand extends AudioCommand {

    public static final String KEY = "discord.command.pause.key";

    @Override
    public boolean doInternal(GuildMessageReceivedEvent message, BotContext context, String content) {
        if (playerService.pause(message.getGuild())) {
            return ok(message, "discord.command.pause.paused");
        }
        return fail(message);
    }
}
