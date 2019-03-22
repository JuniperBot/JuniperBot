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
package ru.caramel.juniperbot.module.audio.commands.control;

import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import ru.caramel.juniperbot.core.command.model.BotContext;
import ru.caramel.juniperbot.core.command.model.DiscordCommand;
import ru.caramel.juniperbot.core.common.model.exception.DiscordException;
import ru.caramel.juniperbot.core.common.model.exception.ValidationException;
import ru.caramel.juniperbot.core.utils.CommonUtils;
import ru.caramel.juniperbot.module.audio.commands.AudioCommand;
import ru.caramel.juniperbot.module.audio.model.PlaybackInstance;

@DiscordCommand(
        key = VolumeCommand.KEY,
        description = "discord.command.volume.desc",
        group = "discord.command.group.music",
        source = ChannelType.TEXT,
        priority = 111)
public class VolumeCommand extends AudioCommand {

    public static final String KEY = "discord.command.volume.key";

    @Override
    protected boolean doInternal(MessageReceivedEvent message, BotContext context, String content) throws DiscordException {
        int volume = parseCount(content);
        PlaybackInstance instance = playerService.getInstance(message.getGuild());
        instance.setVolume(volume);
        if (instance.getCurrent() != null) {
            messageManager.updateMessage(instance.getCurrent());
        }
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
            } else if (count > 150) {
                throw new ValidationException("discord.command.audio.volume.max");
            }
        }
        return count;
    }

    @Override
    public boolean isAvailable(User user, Member member, Guild guild) {
        return guild != null && featureSetService.isAvailable(guild);
    }
}
