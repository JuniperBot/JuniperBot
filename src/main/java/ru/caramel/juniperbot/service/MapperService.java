package ru.caramel.juniperbot.service;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;
import ru.caramel.juniperbot.model.ConfigDto;
import ru.caramel.juniperbot.model.CustomCommandDto;
import ru.caramel.juniperbot.model.VkConnectionDto;
import ru.caramel.juniperbot.model.WebHookDto;
import ru.caramel.juniperbot.persistence.entity.CustomCommand;
import ru.caramel.juniperbot.persistence.entity.GuildConfig;
import ru.caramel.juniperbot.persistence.entity.VkConnection;
import ru.caramel.juniperbot.persistence.entity.WebHook;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MapperService {

    ConfigDto getConfigDto(GuildConfig config);

    VkConnectionDto getVkConnectionDto(VkConnection connection);

    CustomCommandDto getCommandDto(CustomCommand command);

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

    @Mapping(target = "id", ignore = true)
    void updateCommand(CustomCommandDto source, @MappingTarget CustomCommand target);

    void updateWebHook(WebHookDto source, @MappingTarget WebHook target);

    default String trimmed(String s) { return s != null ? s.trim() : null; }
}
