package ru.caramel.juniperbot.service;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;
import ru.caramel.juniperbot.model.ConfigDto;
import ru.caramel.juniperbot.persistence.entity.GuildConfig;

@Mapper(componentModel = "spring")
public interface MapperService {

    ConfigDto getConfigDto(GuildConfig config);

    @Mappings({
            @Mapping(target = "prefix", expression = "java(trimmed(source.getPrefix()))")
    })
    void updateConfig(ConfigDto source, @MappingTarget GuildConfig target);

    default String trimmed(String s) { return s != null ? s.trim() : null; }
}
