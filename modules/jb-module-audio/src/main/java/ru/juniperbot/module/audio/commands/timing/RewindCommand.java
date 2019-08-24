/*
 * This file is part of JuniperBot.
 *
 * JuniperBot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * JuniperBot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with JuniperBot. If not, see <http://www.gnu.org/licenses/>.
 */
package ru.juniperbot.module.audio.commands.timing;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import ru.juniperbot.common.utils.CommonUtils;
import ru.juniperbot.common.worker.command.model.DiscordCommand;
import ru.juniperbot.module.audio.model.PlaybackInstance;
import ru.juniperbot.module.audio.model.TrackRequest;

@DiscordCommand(
        key = "discord.command.rewind.key",
        description = "discord.command.rewind.desc",
        group = "discord.command.group.music",
        priority = 113)
public class RewindCommand extends TimingCommand {

    @Override
    protected boolean doInternal(GuildMessageReceivedEvent message, TrackRequest request, long millis) {
        PlaybackInstance instance = playerService.get(message.getGuild());
        AudioTrack track = request.getTrack();
        long position = instance.getPosition();
        millis = Math.min(position, millis);
        if (instance.seek(position - millis)) {
            messageManager.onMessage(message.getChannel(), "discord.command.audio.rewind",
                    messageManager.getTitle(track.getInfo()), CommonUtils.formatDuration(millis));
            request.setResetMessage(true);
            return true;
        }
        return false;
    }
}
