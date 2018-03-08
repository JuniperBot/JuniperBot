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
package ru.caramel.juniperbot.module.audio.service;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.VoiceChannel;
import ru.caramel.juniperbot.core.model.exception.DiscordException;
import ru.caramel.juniperbot.module.audio.model.PlaybackInstance;
import ru.caramel.juniperbot.module.audio.model.TrackRequest;
import ru.caramel.juniperbot.module.audio.persistence.entity.MusicConfig;
import ru.caramel.juniperbot.module.audio.persistence.entity.Playlist;

import java.util.List;
import java.util.Map;

public interface PlayerService {

    String ACTIVE_CONNECTIONS = "player.activeConnections";

    AudioPlayerManager getPlayerManager();

    MusicConfig getConfig(long serverId);

    MusicConfig getConfig(Guild guild);

    PlaybackInstance getInstance(Guild guild);

    PlaybackInstance getInstance(long guildId, boolean create);

    Map<Long, PlaybackInstance> getInstances();

    void play(List<TrackRequest> requests) throws DiscordException;

    void play(TrackRequest request) throws DiscordException;

    void skipTrack(Member member, Guild guild);

    TrackRequest removeByIndex(Guild guild, int index);

    boolean shuffle(Guild guild);

    boolean isInChannel(Member member);

    boolean hasAccess(Member member);

    VoiceChannel getChannel(Member member);

    void reconnectAll();

    VoiceChannel connectToChannel(PlaybackInstance instance, Member member) throws DiscordException;

    void monitor();

    long getActiveCount();

    boolean stop(Member member, Guild guild);

    Playlist getPlaylist(String uuid);
}
