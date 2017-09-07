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
package ru.caramel.juniperbot.service.impl;

import net.dv8tion.jda.core.entities.*;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.caramel.juniperbot.configuration.DiscordConfig;
import ru.caramel.juniperbot.integration.discord.DiscordClient;
import ru.caramel.juniperbot.model.*;
import ru.caramel.juniperbot.model.enums.VkConnectionStatus;
import ru.caramel.juniperbot.model.enums.WebHookType;
import ru.caramel.juniperbot.persistence.entity.*;
import ru.caramel.juniperbot.persistence.repository.WelcomeMessageRepository;
import ru.caramel.juniperbot.service.MapperService;
import ru.caramel.juniperbot.persistence.repository.GuildConfigRepository;
import ru.caramel.juniperbot.service.ConfigService;
import ru.caramel.juniperbot.service.WebHookService;

import javax.persistence.EntityManager;
import java.util.HashMap;
import java.util.Map;

@Service
public class ConfigServiceImpl implements ConfigService {

    @Autowired
    private GuildConfigRepository repository;

    @Autowired
    private WelcomeMessageRepository welcomeMessageRepository;

    @Autowired
    private EntityManager entityManager;

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
        return createIfMissing(config, serverId);
    }

    @Override
    @Transactional
    public GuildConfig getOrCreate(long serverId, String graph) {
        return createIfMissing(entityManager
                .createNamedQuery(GuildConfig.FIND_BY_GUILD_ID, GuildConfig.class)
                .setParameter("guildId", serverId)
                .setHint(org.springframework.data.jpa.repository.EntityGraph.EntityGraphType.LOAD.getKey(),
                        entityManager.getEntityGraph(graph))
                .getSingleResult(), serverId);
    }

    @Override
    public MusicConfig getMusicConfig(long serverId) {
        return repository.findMusicConfig(serverId);
    }

    @Override
    public String getPrefix(long serverId) {
        String prefix = repository.findPrefixByGuildId(serverId);
        return prefix != null ? prefix : getOrCreate(serverId).getPrefix();
    }

    @Transactional(readOnly = true)
    @Override
    public boolean exists(long serverId) {
        return repository.existsByGuildId(serverId);
    }

    @Transactional(readOnly = true)
    @Override
    public WelcomeMessageDto getWelcomeMessageDto(long serverId) {
        WelcomeMessage welcomeMessage = welcomeMessageRepository.findByGuildId(serverId);
        return welcomeMessage != null ? mapper.getMessageDto(welcomeMessage) : new WelcomeMessageDto();
    }

    @Transactional(readOnly = true)
    @Override
    public WelcomeMessage getWelcomeMessage(long serverId) {
        return welcomeMessageRepository.findByGuildId(serverId);
    }

    @Transactional
    @Override
    public void saveWelcomeMessage(WelcomeMessageDto dto, long serverId) {
        WelcomeMessage welcomeMessage = welcomeMessageRepository.findByGuildId(serverId);
        if (welcomeMessage == null) {
            welcomeMessage = new WelcomeMessage();
            welcomeMessage.setConfig(getOrCreate(serverId));
        }
        mapper.updateWelcomeMessage(dto, welcomeMessage);
        welcomeMessageRepository.save(welcomeMessage);
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

        MusicConfigDto musicConfigDto = new MusicConfigDto();
        if (discordClient.isConnected() && (musicConfigDto.getChannelId() == null ||
                discordClient.getJda().getVoiceChannelById(musicConfigDto.getChannelId()) == null)) {
            VoiceChannel channel = discordClient.getDefaultMusicChannel(config.getGuildId());
            if (channel != null) {
                musicConfigDto.setChannelId(channel.getIdLong());
            }
        }
        return dto;
    }

    private void updateConfig(ConfigDto dto, GuildConfig config) {
        WebHook webHook = config.getWebHook();
        if (webHook == null) {
            webHook = new WebHook();
            webHook.setType(WebHookType.INSTAGRAM);
            config.setWebHook(webHook);
        }
        mapper.updateConfig(dto, config);
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
        webHookService.invalidateCache(config.getGuildId());
    }

    private GuildConfig createIfMissing(GuildConfig config, long serverId) {
        if (config == null) {
            config = new GuildConfig(serverId);
            config.setPrefix(discordConfig.getPrefix());
            config.setMusicConfig(new MusicConfig());
            WebHook webHook = new WebHook();
            webHook.setType(WebHookType.INSTAGRAM);
            config.setWebHook(webHook);
            repository.save(config);
        }
        return config;
    }
}
