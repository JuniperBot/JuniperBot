package ru.caramel.juniperbot.commands.audio.timing;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import ru.caramel.juniperbot.audio.model.TrackRequest;
import ru.caramel.juniperbot.commands.model.CommandGroup;
import ru.caramel.juniperbot.commands.model.CommandSource;
import ru.caramel.juniperbot.commands.model.DiscordCommand;
import ru.caramel.juniperbot.utils.CommonUtils;

@DiscordCommand(
        key = "назад",
        description = "Перемотать воспроизведение назад на заданное время: [[чч:]мм:]сс",
        source = CommandSource.GUILD,
        group = CommandGroup.MUSIC,
        priority = 113)
public class RewindCommand extends TimingCommand {

    @Override
    protected boolean doInternal(MessageReceivedEvent message, TrackRequest request, long millis) {
        AudioTrack track = request.getTrack();
        long position = track.getPosition();

        millis = Math.min(position, millis);

        if (handlerService.seek(message.getGuild(), position - millis)) {
            messageManager.onMessage(message.getChannel(), String.format("**%s** перемотан назад на `%s`", track.getInfo().title,  CommonUtils.formatDuration(millis)));
            request.setResetMessage(true);
        }
        return true;
    }
}
