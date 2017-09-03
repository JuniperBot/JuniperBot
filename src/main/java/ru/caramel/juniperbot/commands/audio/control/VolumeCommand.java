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
package ru.caramel.juniperbot.commands.audio.control;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import ru.caramel.juniperbot.commands.audio.AudioCommand;
import ru.caramel.juniperbot.commands.model.*;
import ru.caramel.juniperbot.integration.discord.model.DiscordException;
import ru.caramel.juniperbot.utils.CommonUtils;

@DiscordCommand(
        key = "discord.command.volume.key",
        description = "discord.command.volume.desc",
        source = CommandSource.GUILD,
        group = CommandGroup.MUSIC,
        priority = 111)
public class VolumeCommand extends AudioCommand {

    @Override
    protected boolean doInternal(MessageReceivedEvent message, BotContext context, String content) throws DiscordException {
        int volume = parseCount(content);
        playerService.getInstance(message.getGuild()).setVolume(volume);
        messageManager.onMessage(message.getChannel(), "discord.command.audio.volume", volume, CommonUtils.getVolumeIcon(volume));
        return true;
    }

    private static int parseCount(String content) throws ValidationException {
        int count = 100;
        if (!content.isEmpty()) {
            try {
                count = Integer.parseInt(content);
            } catch (NumberFormatException e) {
                throw new ValidationException("discord.global.integer.parseError");
            }
            if (count < 0) {
                throw new ValidationException("discord.global.integer.negative");
            } else if (count > 100) {
                throw new ValidationException("discord.command.audio.volume.max");
            }
        }
        return count;
    }
}
