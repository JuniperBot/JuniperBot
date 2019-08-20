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

import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.apache.commons.lang3.StringUtils;
import ru.caramel.juniperbot.module.audio.commands.AudioCommand;
import ru.caramel.juniperbot.module.audio.model.PlaybackInstance;
import ru.caramel.juniperbot.module.audio.model.TrackRequest;
import ru.juniperbot.common.worker.command.model.BotContext;
import ru.juniperbot.common.worker.command.model.DiscordCommand;
import ru.juniperbot.common.worker.utils.DiscordUtils;

@DiscordCommand(
        key = "discord.command.audio.remove.key",
        description = "discord.command.audio.remove.desc",
        group = "discord.command.group.music",
        priority = 120)
public class RemoveCommand extends AudioCommand {

    @Override
    public boolean doInternal(GuildMessageReceivedEvent message, BotContext context, String content) {
        if (StringUtils.isNumeric(content)) {
            Integer index;
            try {
                index = Integer.parseInt(content) - 1;
                if (index >= 0) {
                    PlaybackInstance instance = playerService.get(message.getGuild());
                    if (index.equals(instance.getCursor())) {
                        messageManager.onMessage(message.getChannel(), "discord.command.audio.remove.notCurrent");
                        return false;
                    }
                    TrackRequest request = playerService.removeByIndex(message.getGuild(), index);
                    if (request != null) {
                        messageManager.onMessage(message.getChannel(), "discord.command.audio.remove.done",
                                messageManager.getTitle(request.getTrack().getInfo()),
                                DiscordUtils.getUrl(request.getTrack().getInfo().uri));
                        return true;
                    }
                }
            } catch (NumberFormatException e) {
                // fall down with error
            }
        }
        messageManager.onMessage(message.getChannel(), "discord.command.audio.remove.invalid");
        return false;
    }
}
