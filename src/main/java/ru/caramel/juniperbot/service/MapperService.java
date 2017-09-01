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

    CustomCommand getCommand(CustomCommandDto command);

    List<CustomCommandDto> getCommandsDto(List<CustomCommand> command);

    List<CustomCommand> getCommands(List<CustomCommandDto> command);

    @Mappings({
            @Mapping(target = "prefix", expression = "java(trimmed(source.getPrefix()))"),
            @Mapping(target = "vkConnections", ignore = true)
    })
    void updateConfig(ConfigDto source, @MappingTarget GuildConfig target);

    WebHookDto getWebHookDto(WebHook config);

    @Mapping(target = "id", ignore = true)
    void updateConnection(VkConnectionDto source, @MappingTarget VkConnection target);

    void updateMusicConfig(MusicConfigDto source, @MappingTarget MusicConfig target);

    @Mapping(target = "id", ignore = true)
    void updateCommand(CustomCommandDto source, @MappingTarget CustomCommand target);

    void updateWebHook(WebHookDto source, @MappingTarget WebHook target);

    default String trimmed(String s) { return s != null ? s.trim() : null; }
}
