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
package ru.juniperbot.worker.rabbit;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.VoiceChannel;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.juniperbot.common.configuration.RabbitConfiguration;
import ru.juniperbot.common.model.discord.GuildDto;
import ru.juniperbot.common.service.DiscordMapperService;
import ru.juniperbot.worker.common.feature.service.FeatureSetService;
import ru.juniperbot.worker.common.shared.service.DiscordService;

import static net.dv8tion.jda.api.OnlineStatus.OFFLINE;
import static net.dv8tion.jda.api.OnlineStatus.UNKNOWN;

@EnableRabbit
@Component
@Slf4j
public class GuildInfoQueueListener extends BaseQueueListener {

    @Autowired
    private FeatureSetService featureSetService;

    @Autowired
    private DiscordMapperService discordMapperService;

    @RabbitListener(queues = RabbitConfiguration.QUEUE_GUILD_INFO_REQUEST)
    public GuildDto getGuildInfo(Long guildId) {
        Guild guild = getGuildById(guildId);
        if (guild == null) {
            return GuildDto.EMPTY;
        }

        GuildDto dto = discordMapperService.getGuildDto(guild);
        dto.setFeatureSets(featureSetService.getByGuild(guildId));
        dto.setOnlineCount(guild.getMembers().stream()
                .filter(m -> m.getOnlineStatus() != OFFLINE && m.getOnlineStatus() != UNKNOWN).count());

        VoiceChannel defaultMusicChannel = discordService.getDefaultMusicChannel(guildId);
        if (defaultMusicChannel != null) {
            dto.setDefaultMusicChannelId(defaultMusicChannel.getId());
        }
        return dto;
    }
}
