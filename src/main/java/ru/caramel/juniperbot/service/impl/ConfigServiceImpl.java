package ru.caramel.juniperbot.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.caramel.juniperbot.configuration.DiscordConfig;
import ru.caramel.juniperbot.model.ConfigDto;
import ru.caramel.juniperbot.service.MapperService;
import ru.caramel.juniperbot.persistence.entity.GuildConfig;
import ru.caramel.juniperbot.persistence.repository.GuildConfigRepository;
import ru.caramel.juniperbot.service.ConfigService;

@Service
public class ConfigServiceImpl implements ConfigService {

    @Autowired
    private GuildConfigRepository repository;

    @Autowired
    private DiscordConfig discordConfig;

    @Autowired
    private MapperService mapper;

    @Override
    @Transactional
    public ConfigDto getConfig(long serverId) {
        return mapper.getConfigDto(getOrCreate(serverId));
    }

    @Override
    @Transactional
    public void saveConfig(ConfigDto dto, long serverId) {
        GuildConfig config = getOrCreate(serverId);
        mapper.updateConfig(dto, config);
        repository.save(config);
    }

    @Override
    @Transactional
    public GuildConfig getOrCreate(long serverId) {
        GuildConfig config = repository.findByGuildId(serverId);
        if (config == null) {
            config = new GuildConfig(serverId);
            config.setPrefix(discordConfig.getPrefix());
            repository.save(config);
        }
        return config;
    }
}
