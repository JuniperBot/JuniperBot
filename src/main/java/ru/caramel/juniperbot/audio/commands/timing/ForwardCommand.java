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
package ru.caramel.juniperbot.audio.commands.timing;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import ru.caramel.juniperbot.audio.model.TrackRequest;
import ru.caramel.juniperbot.model.enums.CommandGroup;
import ru.caramel.juniperbot.model.enums.CommandSource;
import ru.caramel.juniperbot.model.DiscordCommand;
import ru.caramel.juniperbot.utils.CommonUtils;

@DiscordCommand(
        key = "discord.command.forward.key",
        description = "discord.command.forward.desc",
        source = CommandSource.GUILD,
        group = CommandGroup.MUSIC,
        priority = 113)
public class ForwardCommand extends TimingCommand {

    @Override
    protected boolean doInternal(MessageReceivedEvent message, TrackRequest request, long millis) {
        AudioTrack track = request.getTrack();
        long duration = track.getDuration();
        long position = track.getPosition();

        millis = Math.min(duration - position, millis);

        if (playerService.getInstance(message.getGuild()).seek(position + millis)) {
            messageManager.onMessage(message.getChannel(), "discord.command.audio.forward",
                    messageManager.getTitle(track.getInfo()), CommonUtils.formatDuration(millis));
            request.setResetMessage(true);
        }
        return true;
    }
}
