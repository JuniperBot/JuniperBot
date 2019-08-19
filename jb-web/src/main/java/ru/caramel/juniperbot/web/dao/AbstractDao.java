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
package ru.caramel.juniperbot.web.dao;

import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.VoiceChannel;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import ru.juniperbot.common.service.ConfigService;
import ru.juniperbot.worker.common.shared.service.DiscordService;
import ru.juniperbot.common.support.JbCacheManager;
import ru.caramel.juniperbot.web.service.ApiMapperService;

public abstract class AbstractDao {

    @Autowired
    protected ApiMapperService apiMapper;

    @Autowired
    protected ConfigService configService;

    @Autowired
    protected DiscordService discordService;

    protected String filterTextChannel(long guildId, String channelId) {
        if (StringUtils.isEmpty(channelId)) {
            return channelId;
        }
        TextChannel channel = discordService.getTextChannelById(channelId);
        if (channel == null) {
            return null;
        }
        return channel.getGuild().getIdLong() == guildId ? channelId : null;
    }


    protected String filterVoiceChannel(long guildId, String channelId) {
        if (StringUtils.isEmpty(channelId)) {
            return channelId;
        }
        VoiceChannel channel = discordService.getVoiceChannelById(channelId);
        if (channel == null) {
            return null;
        }
        return channel.getGuild().getIdLong() == guildId ? channelId : null;
    }
}
