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
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import ru.caramel.juniperbot.core.command.model.BotContext;
import ru.caramel.juniperbot.core.command.model.DiscordCommand;
import ru.caramel.juniperbot.core.common.model.exception.DiscordException;
import ru.caramel.juniperbot.module.audio.commands.AudioCommand;
import ru.caramel.juniperbot.module.audio.model.PlaybackInstance;
import ru.caramel.juniperbot.module.audio.model.RepeatMode;

import java.util.stream.Collectors;
import java.util.stream.Stream;

@DiscordCommand(
        key = RepeatCommand.KEY,
        description = "discord.command.repeat.desc",
        group = "discord.command.group.music",
        source = ChannelType.TEXT,
        priority = 108)
public class RepeatCommand extends AudioCommand {

    public static final String KEY = "discord.command.repeat.key";

    @Override
    protected boolean doInternal(MessageReceivedEvent message, BotContext context, String content) throws DiscordException {
        RepeatMode mode = messageService.getEnumeration(RepeatMode.class, content);
        if (mode == null) {
            messageManager.onMessage(message.getChannel(), "discord.command.audio.repeat.help",
                    Stream.of(RepeatMode.values()).map(messageService::getEnumTitle).collect(Collectors.joining("|")));
            return false;
        }
        PlaybackInstance instance = playerService.getInstance(message.getGuild());
        if (playerService.isActive(message.getGuild())) {
            instance.setMode(mode);
            messageManager.onMessage(message.getChannel(), "discord.command.audio.repeat", mode.getEmoji());
            if (instance.getCurrent() != null) {
                messageManager.updateMessage(instance.getCurrent());
            }
            return ok(message, "discord.command.audio.repeat", messageService.getEnumTitle(mode));
        }
        messageManager.onMessage(message.getChannel(), "discord.command.audio.notStarted");
        return fail(message);
    }
}
