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

import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import org.apache.commons.lang3.StringUtils;
import ru.caramel.juniperbot.core.command.model.BotContext;
import ru.caramel.juniperbot.core.command.model.DiscordCommand;
import ru.caramel.juniperbot.core.common.model.exception.DiscordException;
import ru.caramel.juniperbot.module.audio.commands.AudioCommand;
import ru.caramel.juniperbot.module.audio.model.PlaybackInstance;

@DiscordCommand(
        key = "discord.command.queue.key",
        description = "discord.command.queue.desc",
        group = "discord.command.group.music",
        priority = 101)
public class QueueCommand extends AudioCommand {

    @Override
    public boolean doInternal(GuildMessageReceivedEvent message, BotContext context, String content) throws DiscordException {
        int pageNum = 1;
        if (StringUtils.isNotEmpty(content)) {
            try {
                pageNum = Integer.parseInt(content);
            } catch (Exception e) {
                pageNum = 0;
            }
        }
        if (pageNum < 1) {
            messageManager.onQueueError(message.getChannel(), "discord.command.audio.queue.list.selectPage");
            return fail(message);
        }
        PlaybackInstance instance = playerService.getInstance(message.getGuild());

        messageManager.onQueue(instance, message.getChannel(), context, pageNum);
        return true;
    }

    @Override
    protected boolean isChannelRestricted() {
        return false;
    }
}
