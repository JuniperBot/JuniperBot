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
package ru.caramel.juniperbot.web.service;

import net.dv8tion.jda.core.entities.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;
import ru.caramel.juniperbot.core.persistence.entity.GuildConfig;
import ru.caramel.juniperbot.module.moderation.persistence.entity.ModerationConfig;
import ru.caramel.juniperbot.web.dto.api.discord.GuildShortDto;
import ru.caramel.juniperbot.web.dto.api.config.CommonConfigDto;
import ru.caramel.juniperbot.web.dto.api.config.ModerationConfigDto;
import ru.caramel.juniperbot.web.dto.api.discord.RoleDto;
import ru.caramel.juniperbot.web.security.model.DiscordGuildDetails;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ApiMapperService {

    @Mappings({
            @Mapping(target = "added", ignore = true),
            @Mapping(target = "members", ignore = true)
    })
    GuildShortDto getGuildDto(DiscordGuildDetails details);

    @Mappings({
            @Mapping(target = "interactable", ignore = true)
    })
    RoleDto getRoleDto(Role role);

    List<GuildShortDto> getGuildDtos(List<DiscordGuildDetails> details);

    @Mappings({
            @Mapping(target = "modConfig", ignore = true)
    })
    CommonConfigDto getCommonDto(GuildConfig config);

    @Mappings({
            @Mapping(target = "prefix", expression = "java(trimmed(source.getPrefix()))"),
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "version", ignore = true),
            @Mapping(target = "guildId", ignore = true),
            @Mapping(target = "name", ignore = true),
            @Mapping(target = "iconUrl", ignore = true),
            @Mapping(target = "disabledCommands", ignore = true)
    })
    void updateCommon(CommonConfigDto source, @MappingTarget GuildConfig target);

    ModerationConfigDto getModerationDto(ModerationConfig moderationConfig);

    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "version", ignore = true),
            @Mapping(target = "guildConfig", ignore = true)
    })
    void updateModerationConfig(ModerationConfigDto source, @MappingTarget ModerationConfig target);

    default String trimmed(String s) {
        return s != null ? s.trim() : null;
    }
}
