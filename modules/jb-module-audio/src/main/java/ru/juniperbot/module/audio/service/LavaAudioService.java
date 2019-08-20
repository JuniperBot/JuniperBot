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

import lavalink.client.io.jda.JdaLavalink;
import lavalink.client.player.IPlayer;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.VoiceChannel;
import ru.juniperbot.common.worker.shared.service.AudioService;
import ru.juniperbot.module.audio.model.LavaLinkConfiguration;

public interface LavaAudioService extends AudioService {

    IPlayer createPlayer(String guildId);

    void openConnection(IPlayer player, VoiceChannel channel);

    void closeConnection(Guild guild);

    JdaLavalink getLavaLink();

    void shutdown();

    boolean isConnected(Guild guild);

    LavaLinkConfiguration getConfiguration();
}
