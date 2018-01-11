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
package ru.caramel.juniperbot.modules.audio.commands;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import ru.caramel.juniperbot.modules.audio.service.PlaybackInstance;
import ru.caramel.juniperbot.core.model.exception.DiscordException;
import ru.caramel.juniperbot.core.model.BotContext;
import ru.caramel.juniperbot.core.model.DiscordCommand;
import ru.caramel.juniperbot.core.model.enums.CommandGroup;
import ru.caramel.juniperbot.core.model.enums.CommandSource;
import ru.caramel.juniperbot.modules.audio.persistence.entity.MusicConfig;
import ru.caramel.juniperbot.core.service.ConfigService;

@DiscordCommand(
        key = "discord.command.here.key",
        description = "discord.command.here.desc",
        source = CommandSource.GUILD,
        group = CommandGroup.MUSIC,
        priority = 104)
public class HereCommand extends AudioCommand {

    @Autowired
    private ConfigService configService;

    @Override
    protected boolean doInternal(MessageReceivedEvent message, BotContext context, String content) throws DiscordException {
        MusicConfig musicConfig = configService.getMusicConfig(message.getGuild().getIdLong());
        if (musicConfig == null || !musicConfig.isUserJoinEnabled()) {
            messageService.onError(message.getTextChannel(), "discord.command.here.denied");
            return false;
        }
        if (!message.getMember().getVoiceState().inVoiceChannel()) {
            messageService.onError(message.getTextChannel(), "discord.command.here.notInChannel");
            return false;
        }
        PlaybackInstance instance = playerService.getInstance(message.getGuild());
        if (!instance.isActive()) {
            messageService.onError(message.getTextChannel(), "discord.command.audio.notStarted");
            return false;
        }
        playerService.connectToChannel(instance, message.getMember());
        return true;
    }

    @Override
    protected boolean isChannelRestricted() {
        return false;
    }
}
