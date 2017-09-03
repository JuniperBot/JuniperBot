package ru.caramel.juniperbot.service;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;
import ru.caramel.juniperbot.model.*;
import ru.caramel.juniperbot.persistence.entity.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MapperService {

    ConfigDto getConfigDto(GuildConfig config);

    VkConnectionDto getVkConnectionDto(VkConnection connection);

    CustomCommandDto getCommandDto(CustomCommand command);

    MusicConfigDto getMusicDto(MusicConfig musicConfig);

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

    default String trimmed(String s) { return s != null ? s.trim() : null; }
}
