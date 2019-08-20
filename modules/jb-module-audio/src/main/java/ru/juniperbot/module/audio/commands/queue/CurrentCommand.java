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
package ru.juniperbot.module.audio.commands.queue;

import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import ru.juniperbot.common.worker.command.model.BotContext;
import ru.juniperbot.common.worker.command.model.DiscordCommand;
import ru.juniperbot.module.audio.commands.AudioCommand;
import ru.juniperbot.module.audio.model.TrackRequest;

@DiscordCommand(
        key = "discord.command.current.key",
        description = "discord.command.current.desc",
        group = "discord.command.group.music",
        priority = 102)
public class CurrentCommand extends AudioCommand {
    @Override
    protected boolean doInternal(GuildMessageReceivedEvent message, BotContext context, String content) {
        TrackRequest current = playerService.get(message.getGuild()).getCurrent();
        if (current == null) {
            messageManager.onMessage(message.getChannel(), "discord.command.audio.notStarted");
            return fail(message);
        }
        messageManager.onResetMessage(current);
        return true;
    }

    @Override
    protected boolean isChannelRestricted() {
        return false;
    }
}
