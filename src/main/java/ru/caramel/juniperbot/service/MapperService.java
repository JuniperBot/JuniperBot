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

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;
import ru.caramel.juniperbot.model.dto.*;
import ru.caramel.juniperbot.persistence.entity.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MapperService {

    ConfigDto getConfigDto(GuildConfig config);

    VkConnectionDto getVkConnectionDto(VkConnection connection);

    CustomCommandDto getCommandDto(CustomCommand command);

    MusicConfigDto getMusicDto(MusicConfig musicConfig);

    WelcomeMessageDto getMessageDto(WelcomeMessage welcomeMessage);

    @Mappings({
            @Mapping(target = "version", ignore = true),
            @Mapping(target = "config", ignore = true),
    })
    CustomCommand getCommand(CustomCommandDto command);

    List<CustomCommandDto> getCommandsDto(List<CustomCommand> command);

    List<CustomCommand> getCommands(List<CustomCommandDto> command);

    @Mappings({
            @Mapping(target = "prefix", expression = "java(trimmed(source.getPrefix()))"),
            @Mapping(target = "vkConnections", ignore = true),
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "version", ignore = true),
            @Mapping(target = "guildId", ignore = true),
            @Mapping(target = "commands", ignore = true)
    })
    void updateConfig(ConfigDto source, @MappingTarget GuildConfig target);

    @Mappings({
            @Mapping(target = "available", ignore = true),
            @Mapping(target = "channelId", ignore = true),
    })
    WebHookDto getWebHookDto(WebHook config);

    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "version", ignore = true),
            @Mapping(target = "config", ignore = true),
            @Mapping(target = "groupId", ignore = true),
            @Mapping(target = "confirmCode", ignore = true)
    })
    void updateConnection(VkConnectionDto source, @MappingTarget VkConnection target);

    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "version", ignore = true)
    })
    void updateMusicConfig(MusicConfigDto source, @MappingTarget MusicConfig target);

    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "version", ignore = true),
            @Mapping(target = "config", ignore = true)
    })
    void updateCommand(CustomCommandDto source, @MappingTarget CustomCommand target);

    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "version", ignore = true),
            @Mapping(target = "hookId", ignore = true),
            @Mapping(target = "token", ignore = true),
            @Mapping(target = "type", ignore = true)
    })
    void updateWebHook(WebHookDto source, @MappingTarget WebHook target);

    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "version", ignore = true),
            @Mapping(target = "config", ignore = true)
    })
    void updateWelcomeMessage(WelcomeMessageDto source, @MappingTarget WelcomeMessage target);

    default String trimmed(String s) { return s != null ? s.trim() : null; }
}
