package ru.caramel.juniperbot.service;

import ru.caramel.juniperbot.model.ConfigDto;
import ru.caramel.juniperbot.persistence.entity.GuildConfig;

public interface ConfigService {

    boolean exists(long serverId);

    ConfigDto getConfig(long serverId);

    void saveConfig(ConfigDto dto, long serverId);

    void save(GuildConfig config);

    GuildConfig getOrCreate(long serverId);
}
