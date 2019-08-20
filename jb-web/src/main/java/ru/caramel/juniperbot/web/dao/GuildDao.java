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

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.caramel.juniperbot.web.dto.GuildInfoDto;
import ru.caramel.juniperbot.web.dto.ShortMemberDto;
import ru.caramel.juniperbot.web.dto.request.GuildInfoRequest;
import ru.caramel.juniperbot.web.security.auth.DiscordTokenServices;
import ru.caramel.juniperbot.web.security.utils.SecurityUtils;
import ru.juniperbot.common.model.discord.GuildDto;
import ru.juniperbot.common.persistence.entity.GuildConfig;
import ru.juniperbot.common.persistence.entity.LocalUser;
import ru.juniperbot.common.service.MemberService;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static ru.caramel.juniperbot.web.dto.request.GuildInfoRequest.PartType.*;

@Service
public class GuildDao extends AbstractDao {

    @Autowired
    private DiscordTokenServices tokenServices;

    @Autowired
    private MemberService memberService;

    @Transactional
    public GuildInfoDto getGuild(GuildInfoRequest request) {
        GuildConfig config = configService.getByGuildId(request.getId());
        return getGuild(config, request.getParts());
    }

    @Transactional
    public GuildInfoDto getGuild(long guildId) {
        GuildConfig config = configService.getByGuildId(guildId);
        return getGuild(config, null);
    }

    @Transactional(readOnly = true)
    public List<ShortMemberDto> getMembers(long guildId, String query) {
        return memberService.findLike(guildId, query).stream().map(e -> {
            ShortMemberDto dto = new ShortMemberDto();
            LocalUser user = e.getUser();
            dto.setId(user.getUserId());
            dto.setName(user.getName());
            dto.setDiscriminator(user.getDiscriminator());
            dto.setEffectiveName(e.getEffectiveName());
            dto.setAvatarUrl(user.getAvatarUrl());
            return dto;
        }).collect(Collectors.toList());
    }

    @Transactional
    public GuildInfoDto getGuild(GuildConfig config, Set<GuildInfoRequest.PartType> parts) {
        if (config == null) {
            return null;
        }
        GuildInfoDto.Builder builder = GuildInfoDto.builder()
                .name(config.getName())
                .prefix(config.getPrefix())
                .locale(config.getLocale())
                .color(config.getColor())
                .commandLocale(config.getCommandLocale())
                .id(String.valueOf(config.getGuildId()))
                .icon(config.getIconUrl())
                .timeZone(config.getTimeZone());

        GuildDto guildDto = gatewayService.getGuildInfo(config.getGuildId());
        if (guildDto == null) {
            return builder.build();
        }

        builder.featureSets(guildDto.getFeatureSets());

        if (!guildDto.isAvailable()) {
            return builder.build();
        }

        builder.name(guildDto.getName())
                .icon(guildDto.getIconUrl())
                .available(true);

        if (CollectionUtils.isEmpty(parts)
                || !SecurityUtils.isAuthenticated()
                || !tokenServices.hasPermission(config.getGuildId())) {
            return builder.build();
        }

        if (parts.contains(ROLES)) {
            builder.roles(guildDto.getRoles());
        }

        if (parts.contains(TEXT_CHANNELS)) {
            builder.textChannels(guildDto.getTextChannels());
        }

        if (parts.contains(VOICE_CHANNELS)) {
            builder.voiceChannels(guildDto.getVoiceChannels());
        }
        return builder.build();
    }
}
