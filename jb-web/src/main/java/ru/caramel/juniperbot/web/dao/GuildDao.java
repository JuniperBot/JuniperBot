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

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.caramel.juniperbot.core.common.persistence.GuildConfig;
import ru.caramel.juniperbot.core.common.persistence.LocalUser;
import ru.caramel.juniperbot.core.common.service.MemberService;
import ru.caramel.juniperbot.core.feature.service.FeatureSetService;
import ru.caramel.juniperbot.web.dto.ShortMemberDto;
import ru.caramel.juniperbot.web.dto.discord.GuildDto;
import ru.caramel.juniperbot.web.dto.discord.RoleDto;
import ru.caramel.juniperbot.web.dto.discord.TextChannelDto;
import ru.caramel.juniperbot.web.dto.discord.VoiceChannelDto;
import ru.caramel.juniperbot.web.dto.request.GuildInfoRequest;
import ru.caramel.juniperbot.web.security.auth.DiscordTokenServices;
import ru.caramel.juniperbot.web.security.utils.SecurityUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class GuildDao extends AbstractDao {

    @Autowired
    private DiscordTokenServices tokenServices;

    @Autowired
    private FeatureSetService featureSetService;

    @Autowired
    private MemberService memberService;

    @Transactional
    public GuildDto getGuild(GuildInfoRequest request) {
        GuildConfig config = getOrCreate(request.getId());
        return getGuild(config, request.getParts());
    }

    @Transactional
    public GuildDto getGuild(long guildId) {
        GuildConfig config = getOrCreate(guildId);
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

    private GuildConfig getOrCreate(long guildId) {
        GuildConfig config = configService.getByGuildId(guildId);
        if (config != null) {
            return config;
        }
        if (!discordService.isConnected(guildId)) {
            return null;
        }
        Guild guild = discordService.getGuildById(guildId);
        return guild != null ? configService.getOrCreate(guild) : null;
    }

    @Transactional
    public GuildDto getGuild(GuildConfig config, Set<GuildInfoRequest.PartType> parts) {
        if (config == null) {
            return null;
        }
        GuildDto.Builder builder = GuildDto.builder()
                .name(config.getName())
                .prefix(config.getPrefix())
                .locale(config.getLocale())
                .color(config.getColor())
                .commandLocale(config.getCommandLocale())
                .id(String.valueOf(config.getGuildId()))
                .icon(config.getIconUrl())
                .featureSets(featureSetService.getByGuild(config.getGuildId()));

        if (!discordService.isConnected()) {
            return builder.build();
        }

        Guild guild = discordService.getGuildById(config.getGuildId());
        if (guild == null || !guild.isAvailable()) {
            return builder.build();
        }

        builder.name(guild.getName())
                .id(guild.getId())
                .icon(guild.getIconUrl())
                .available(true);

        if (CollectionUtils.isEmpty(parts)
                || !SecurityUtils.isAuthenticated()
                || !tokenServices.hasPermission(guild.getIdLong())) {
            return builder.build();
        }

        for (GuildInfoRequest.PartType part : parts) {
            switch (part) {
                case ROLES:
                    builder.roles(guild.getRoles().stream()
                            .filter(e -> !e.isPublicRole() && !e.isManaged())
                            .map(e -> {
                                RoleDto dto = apiMapper.getRoleDto(e);
                                dto.setInteractable(guild.getSelfMember().canInteract(e));
                                return dto;
                            })
                            .collect(Collectors.toList()));
                    break;

                case TEXT_CHANNELS:
                    builder.textChannels(guild.getTextChannels().stream()
                        .map(e -> {
                            TextChannelDto dto = apiMapper.getTextChannelDto(e);
                            dto.setPermissions(Permission.getRaw(guild.getSelfMember().getPermissions(e)));
                            return dto;
                        }).collect(Collectors.toList()));
                    break;

                case VOICE_CHANNELS:
                    builder.voiceChannels(guild.getVoiceChannels().stream()
                            .map(e -> {
                                VoiceChannelDto dto = apiMapper.getVoiceChannelDto(e);
                                dto.setPermissions(Permission.getRaw(guild.getSelfMember().getPermissions(e)));
                                return dto;
                            }).collect(Collectors.toList()));
                    break;
            }
        }
        return builder.build();
    }
}
