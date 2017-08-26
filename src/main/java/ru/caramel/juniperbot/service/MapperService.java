package ru.caramel.juniperbot.service;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;
import ru.caramel.juniperbot.model.ConfigDto;
import ru.caramel.juniperbot.model.VkConnectionDto;
import ru.caramel.juniperbot.model.WebHookDto;
import ru.caramel.juniperbot.persistence.entity.GuildConfig;
import ru.caramel.juniperbot.persistence.entity.VkConnection;
import ru.caramel.juniperbot.persistence.entity.WebHook;

@Mapper(componentModel = "spring")
public interface MapperService {

    ConfigDto getConfigDto(GuildConfig config);

    VkConnectionDto getVkConnectionDto(VkConnection connection);

    @Mappings({
            @Mapping(target = "prefix", expression = "java(trimmed(source.getPrefix()))"),
            @Mapping(target = "vkConnections", ignore = true)
    })
    void updateConfig(ConfigDto source, @MappingTarget GuildConfig target);

    WebHookDto getWebHookDto(WebHook config);

    void updateConnection(VkConnectionDto source, @MappingTarget VkConnection target);

    void updateWebHook(WebHookDto source, @MappingTarget WebHook target);

    default String trimmed(String s) { return s != null ? s.trim() : null; }
}
