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
package ru.juniperbot.module.audio.service;

import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.VoiceChannel;
import ru.juniperbot.common.model.exception.DiscordException;
import ru.juniperbot.module.audio.model.PlaybackInstance;
import ru.juniperbot.module.audio.model.TrackRequest;

import java.util.List;
import java.util.Map;

public interface PlayerService {

    String ACTIVE_CONNECTIONS = "player.activeConnections";

    Map<Long, PlaybackInstance> getInstances();

    PlaybackInstance getOrCreate(Guild guild);

    PlaybackInstance get(Guild guild);

    PlaybackInstance get(long guildId, boolean create);

    void loadAndPlay(final TextChannel channel, final Member requestedBy, final String trackUrl);

    void play(AudioPlaylist playlist, List<TrackRequest> requests) throws DiscordException;

    void play(TrackRequest request) throws DiscordException;

    void skipTrack(Member member, Guild guild);

    TrackRequest removeByIndex(Guild guild, int index);

    boolean shuffle(Guild guild);

    boolean isInChannel(Member member);

    boolean hasAccess(Member member);

    VoiceChannel getChannel(Member member);

    VoiceChannel connectToChannel(PlaybackInstance instance, Member member) throws DiscordException;

    VoiceChannel getConnectedChannel(Guild guild);

    VoiceChannel getConnectedChannel(long guildId);

    VoiceChannel getDesiredChannel(Member member);

    void monitor();

    long getActiveCount();

    boolean stop(Member member, Guild guild);

    boolean pause(Guild guild);

    boolean resume(Guild guild, boolean resetTrack);

    boolean isActive(Guild guild);

    boolean isActive(PlaybackInstance instance);
}
