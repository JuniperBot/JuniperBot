package ru.caramel.juniperbot.service.impl;

import net.dv8tion.jda.core.entities.*;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.caramel.juniperbot.configuration.DiscordConfig;
import ru.caramel.juniperbot.integration.discord.DiscordClient;
import ru.caramel.juniperbot.model.*;
import ru.caramel.juniperbot.model.enums.WebHookType;
import ru.caramel.juniperbot.persistence.entity.MusicConfig;
import ru.caramel.juniperbot.persistence.entity.VkConnection;
import ru.caramel.juniperbot.persistence.entity.WebHook;
import ru.caramel.juniperbot.service.MapperService;
import ru.caramel.juniperbot.persistence.entity.GuildConfig;
import ru.caramel.juniperbot.persistence.repository.GuildConfigRepository;
import ru.caramel.juniperbot.service.ConfigService;
import ru.caramel.juniperbot.service.WebHookService;

import java.util.HashMap;
import java.util.Map;

@Service
public class ConfigServiceImpl implements ConfigService {

    @Autowired
    private GuildConfigRepository repository;

    @Autowired
    private DiscordConfig discordConfig;

    @Autowired
    private MapperService mapper;

    @Autowired
    private DiscordClient discordClient;

    @Autowired
    private WebHookService webHookService;

    @Override
    @Transactional
    public ConfigDto getConfig(long serverId) {
        return getConfigDto(getOrCreate(serverId));
    }

    @Override
    @Transactional
    public void saveConfig(ConfigDto dto, long serverId) {
        GuildConfig config = getOrCreate(serverId);
        updateConfig(dto, config);
        repository.save(config);
    }

    @Override
    @Transactional
    public void save(GuildConfig config) {
        repository.save(config);
    }

    @Override
    @Transactional
    public GuildConfig getOrCreate(long serverId) {
        GuildConfig config = repository.findByGuildId(serverId);

        boolean shouldSave = false;
        if (config == null) {
            config = new GuildConfig(serverId);
            config.setPrefix(discordConfig.getPrefix());
            shouldSave = true;
        }

        if (config.getWebHook() == null) {
            WebHook webHook = new WebHook();
            webHook.setType(WebHookType.INSTAGRAM);
            config.setWebHook(webHook);
            shouldSave = true;
        }

        MusicConfig musicConfig = config.getMusicConfig();
        if (musicConfig == null) {
            config.setMusicConfig(musicConfig = new MusicConfig());
        }

        if (discordClient.isConnected() && (musicConfig.getChannelId() == null ||
                discordClient.getJda().getVoiceChannelById(musicConfig.getChannelId()) == null)) {
            VoiceChannel channel = discordClient.getDefaultMusicChannel(config.getGuildId());
            if (channel != null) {
                musicConfig.setChannelId(channel.getIdLong());
                shouldSave = true;
            }
        }
        return shouldSave ? repository.save(config) : config;
    }

    private ConfigDto getConfigDto(GuildConfig config) {
        ConfigDto dto = mapper.getConfigDto(config);
        WebHook webHook = config.getWebHook();
        WebHookDto hookDto = webHookService.getDtoForView(config.getGuildId(), webHook);
        dto.setWebHook(hookDto);
        if (CollectionUtils.isNotEmpty(config.getVkConnections())) {
            for (VkConnection connection : config.getVkConnections()) {
                WebHookDto vkHookDto = webHookService.getDtoForView(config.getGuildId(), connection.getWebHook());
                dto.getVkConnections().stream()
                        .filter(e -> e.getId().equals(connection.getId()))
                        .findFirst()
                        .ifPresent(e -> e.setWebHook(vkHookDto));
            }
        }
        return dto;
    }

    private void updateConfig(ConfigDto dto, GuildConfig config) {
        mapper.updateConfig(dto, config);
        WebHook webHook = config.getWebHook();
        WebHookDto hookDto = dto.getWebHook();

        if (hookDto != null) {
            webHookService.updateWebHook(config.getGuildId(), hookDto.getChannelId(), webHook, "JuniperBot");
        }

        if (dto.getVkConnections() != null) {
            Map<VkConnection, Long> updateMap = new HashMap<>();
            dto.getVkConnections().forEach(e -> {
                VkConnection connection = config.getVkConnections().stream()
                        .filter(e1 -> e.getId().equals(e1.getId()))
                        .findFirst().orElse(null);
                if (connection != null
                        && e.getWebHook() != null
                        && e.getWebHook().getChannelId() != null
                        && VkConnectionStatus.CONNECTED.equals(connection.getStatus())) {
                    mapper.updateWebHook(e.getWebHook(), connection.getWebHook());
                    updateMap.put(connection, e.getWebHook().getChannelId());
                }
            });
            updateMap.forEach((k, v) -> webHookService.updateWebHook(config.getGuildId(), v, k.getWebHook(), k.getName()));
        }
    }
}
