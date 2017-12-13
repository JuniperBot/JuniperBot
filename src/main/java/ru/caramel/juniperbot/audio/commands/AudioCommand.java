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
package ru.caramel.juniperbot.audio.commands;

import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import ru.caramel.juniperbot.audio.service.PlayerService;
import ru.caramel.juniperbot.audio.service.AudioMessageManager;
import ru.caramel.juniperbot.model.Command;
import ru.caramel.juniperbot.model.BotContext;
import ru.caramel.juniperbot.model.exception.ValidationException;
import ru.caramel.juniperbot.integration.discord.model.DiscordException;
import ru.caramel.juniperbot.service.MessageService;

public abstract class AudioCommand implements Command {

    @Autowired
    protected PlayerService playerService;

    @Autowired
    protected AudioMessageManager messageManager;

    @Autowired
    protected MessageService messageService;

    protected abstract boolean doInternal(MessageReceivedEvent message, BotContext context, String content) throws DiscordException;

    @Override
    public boolean doCommand(MessageReceivedEvent message, BotContext context, String content) throws DiscordException {
        if (isChannelRestricted() && !playerService.isInChannel(message.getMember())) {
            VoiceChannel channel = playerService.getChannel(message.getMember());
            throw new ValidationException("discord.command.audio.joinChannel", channel.getName());
        }
        doInternal(message, context, content);
        return false;
    }

    protected boolean isChannelRestricted() {
        return true;
    }
}
