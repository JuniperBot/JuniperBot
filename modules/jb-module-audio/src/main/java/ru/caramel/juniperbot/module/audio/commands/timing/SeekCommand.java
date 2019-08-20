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
package ru.caramel.juniperbot.module.audio.commands.timing;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import ru.caramel.juniperbot.module.audio.model.TrackRequest;
import ru.juniperbot.common.utils.CommonUtils;
import ru.juniperbot.common.worker.command.model.DiscordCommand;

@DiscordCommand(
        key = "discord.command.seek.key",
        description = "discord.command.seek.desc",
        group = "discord.command.group.music",
        priority = 114)
public class SeekCommand extends TimingCommand {

    @Override
    protected boolean doInternal(GuildMessageReceivedEvent message, TrackRequest request, long millis) {
        AudioTrack track = request.getTrack();
        long duration = track.getDuration();
        millis = Math.min(duration, millis);
        if (playerService.get(message.getGuild()).seek(millis)) {
            messageManager.onMessage(message.getChannel(), "discord.command.audio.seek",
                    messageManager.getTitle(track.getInfo()), CommonUtils.formatDuration(millis));
            request.setResetMessage(true);
            return true;
        }
        return fail(message);
    }
}
