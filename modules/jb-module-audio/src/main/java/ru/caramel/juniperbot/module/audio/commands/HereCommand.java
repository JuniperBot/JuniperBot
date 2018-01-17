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
package ru.caramel.juniperbot.module.audio.commands;

import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import ru.caramel.juniperbot.core.model.BotContext;
import ru.caramel.juniperbot.core.model.DiscordCommand;
import ru.caramel.juniperbot.core.model.enums.CommandSource;
import ru.caramel.juniperbot.core.model.exception.DiscordException;
import ru.caramel.juniperbot.core.persistence.entity.GuildConfig;
import ru.caramel.juniperbot.module.audio.model.PlaybackInstance;
import ru.caramel.juniperbot.module.audio.persistence.entity.MusicConfig;

@DiscordCommand(
        key = "discord.command.here.key",
        description = "discord.command.here.desc",
        group = "discord.command.group.music",
        source = CommandSource.GUILD,
        priority = 104)
public class HereCommand extends AudioCommand {

    @Override
    protected boolean doInternal(MessageReceivedEvent message, BotContext context, String content) throws DiscordException {
        if (!message.getMember().getVoiceState().inVoiceChannel()) {
            messageService.onError(message.getTextChannel(), "discord.command.here.notInChannel");
            return fail(message);
        }
        PlaybackInstance instance = playerService.getInstance(message.getGuild());
        if (!instance.isActive()) {
            messageService.onError(message.getTextChannel(), "discord.command.audio.notStarted");
            return fail(message);
        }
        VoiceChannel channel = playerService.connectToChannel(instance, message.getMember());
        if (channel != null) {
            return ok(message, "discord.command.here.connected", channel.getName());
        }
        return fail(message, "discord.command.here.error");
    }

    @Override
    public boolean isAvailable(GuildConfig config) {
        MusicConfig musicConfig = playerService.getConfig(config.getGuildId());
        return musicConfig != null && musicConfig.isUserJoinEnabled();
    }

    @Override
    protected boolean isChannelRestricted() {
        return false;
    }
}
