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
package ru.caramel.juniperbot.service;

import ru.caramel.juniperbot.model.dto.ConfigDto;
import ru.caramel.juniperbot.model.dto.WelcomeMessageDto;
import ru.caramel.juniperbot.persistence.entity.GuildConfig;
import ru.caramel.juniperbot.persistence.entity.MusicConfig;
import ru.caramel.juniperbot.persistence.entity.WelcomeMessage;

public interface ConfigService {

    boolean exists(long serverId);

    ConfigDto getConfig(long serverId);

    void saveConfig(ConfigDto dto, long serverId);

    void save(GuildConfig config);

    GuildConfig getOrCreate(long serverId);

    GuildConfig getOrCreate(long serverId, String graph);

    MusicConfig getMusicConfig(long serverId);

    WelcomeMessage getWelcomeMessage(long serverId);

    WelcomeMessageDto getWelcomeMessageDto(long serverId);

    void saveWelcomeMessage(WelcomeMessageDto dto, long serverId);

    String getPrefix(long serverId);
}
