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

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import ru.juniperbot.common.persistence.entity.MusicConfig;
import ru.juniperbot.worker.common.command.model.BotContext;
import ru.juniperbot.worker.common.command.model.DiscordCommand;
import ru.juniperbot.common.model.exception.DiscordException;
import ru.caramel.juniperbot.module.audio.model.PlaybackInstance;

@DiscordCommand(
        key = "discord.command.here.key",
        description = "discord.command.here.desc",
        group = "discord.command.group.music",
        priority = 104)
public class HereCommand extends AudioCommand {

    @Override
    protected boolean doInternal(GuildMessageReceivedEvent message, BotContext context, String content) throws DiscordException {
        if (!message.getMember().getVoiceState().inVoiceChannel()) {
            messageService.onError(message.getChannel(), "discord.command.here.notInChannel");
            return fail(message);
        }
        PlaybackInstance instance = playerService.get(message.getGuild());
        VoiceChannel channel = playerService.connectToChannel(instance, message.getMember());
        if (channel != null) {
            return ok(message, "discord.command.here.connected", channel.getName());
        }
        return fail(message, "discord.command.here.error");
    }

    @Override
    public boolean isAvailable(User user, Member member, Guild guild) {
        if (guild == null) {
            return false;
        }
        MusicConfig musicConfig = musicConfigService.getByGuildId(guild.getIdLong());
        return musicConfig != null && musicConfig.isUserJoinEnabled();
    }

    @Override
    protected boolean isChannelRestricted() {
        return false;
    }
}
