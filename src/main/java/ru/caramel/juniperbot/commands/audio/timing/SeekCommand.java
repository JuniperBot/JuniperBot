package ru.caramel.juniperbot.commands.audio.timing;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import ru.caramel.juniperbot.audio.model.TrackRequest;
import ru.caramel.juniperbot.commands.model.CommandGroup;
import ru.caramel.juniperbot.commands.model.CommandSource;
import ru.caramel.juniperbot.commands.model.DiscordCommand;
import ru.caramel.juniperbot.utils.CommonUtils;

@DiscordCommand(
        key = "discord.command.seek.key",
        description = "discord.command.seek.desc",
        source = CommandSource.GUILD,
        group = CommandGroup.MUSIC,
        priority = 114)
public class SeekCommand extends TimingCommand {

    @Override
    protected boolean doInternal(MessageReceivedEvent message, TrackRequest request, long millis) {
        AudioTrack track = request.getTrack();
        long duration = track.getDuration();
        millis = Math.min(duration, millis);
        if (playerService.getInstance(message.getGuild()).seek(millis)) {
            messageManager.onMessage(message.getChannel(), "discord.command.audio.seek", track.getInfo().title,  CommonUtils.formatDuration(millis));
            request.setResetMessage(true);
        }
        return true;
    }
}
