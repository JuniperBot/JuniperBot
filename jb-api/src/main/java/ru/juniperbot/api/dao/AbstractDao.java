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
package ru.juniperbot.api.dao;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import ru.juniperbot.api.service.ApiMapperService;
import ru.juniperbot.common.model.discord.GuildDto;
import ru.juniperbot.common.service.ConfigService;
import ru.juniperbot.common.service.GatewayService;

public abstract class AbstractDao {

    @Autowired
    protected ApiMapperService apiMapper;

    @Autowired
    protected ConfigService configService;

    @Autowired
    protected GatewayService gatewayService;

    protected String filterTextChannel(long guildId, String channelId) {
        if (StringUtils.isEmpty(channelId)) {
            return channelId;
        }
        GuildDto guildDto = gatewayService.getGuildInfo(guildId);
        if (guildDto == null) {
            return null;
        }
        return guildDto.getTextChannels().stream().anyMatch(e -> channelId.equals(e.getId())) ? channelId : null;
    }

    protected String filterVoiceChannel(long guildId, String channelId) {
        if (StringUtils.isEmpty(channelId)) {
            return channelId;
        }
        GuildDto guildDto = gatewayService.getGuildInfo(guildId);
        if (guildDto == null) {
            return null;
        }
        return guildDto.getVoiceChannels().stream().anyMatch(e -> channelId.equals(e.getId())) ? channelId : null;
    }
}
